package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.booking.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.booking.infrastructure.TokensDePrueba;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * {@code PUT /internal/v1/cuentas/{id}} (ADR-008) contra PostgreSQL: solo el JWT de servicio con {@code aud = ms-booking},
 * *upsert* idempotente por versión, validación del cuerpo y los CHECK de la tabla {@code cuenta} (V4).
 */
@PruebaIntegracion
class CuentaInternaApiIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private static String cuerpo(String rol, String estado, long version) {
        return "{\"rol\":\"" + rol + "\",\"estadoCuenta\":\"" + estado + "\",\"version\":" + version + "}";
    }

    private MvcResult enviar(UUID id, String autorizacion, String json) throws Exception {
        var solicitud = put("/internal/v1/cuentas/" + id).contentType(MediaType.APPLICATION_JSON).content(json);
        if (autorizacion != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, autorizacion);
        }
        return mvc.perform(solicitud).andReturn();
    }

    private static String servicio() {
        return TokensDePrueba.bearer(TokensDePrueba.servicio("ms-booking"));
    }

    private Map<String, Object> fila(UUID id) {
        List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT rol, estado, version, actualizado_en FROM cuenta WHERE id_usuario = ?", id);
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Test
    void sinTokenResponde401() throws Exception {
        MvcResult resultado = enviar(UUID.randomUUID(), null, cuerpo("Turista", "Activo", 1));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(texto(resultado), "$.codigo")).isEqualTo("NO_AUTENTICADO");
    }

    @Test
    void unTokenDeUsuarioResponde403() throws Exception {
        UUID id = UUID.randomUUID();

        for (String token : List.of(TokensDePrueba.turista(UUID.randomUUID()),
                TokensDePrueba.prestador(UUID.randomUUID()))) {
            MvcResult resultado = enviar(id, TokensDePrueba.bearer(token), cuerpo("Turista", "Desactivado", 1));
            assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
            assertThat(JsonPath.<String>read(texto(resultado), "$.codigo")).isEqualTo("ACCESO_DENEGADO");
        }
        assertThat(fila(id)).isNull();
    }

    @Test
    void unTokenDeServicioConOtraAudienciaResponde401() throws Exception {
        UUID id = UUID.randomUUID();

        MvcResult resultado = enviar(id, TokensDePrueba.bearer(TokensDePrueba.servicio("ms-catalog")),
                cuerpo("Turista", "Desactivado", 1));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(texto(resultado), "$.codigo")).isEqualTo("NO_AUTENTICADO");
        assertThat(fila(id)).isNull();
    }

    @Test
    void unTokenDeServicioValidoGuardaLaProyeccion() throws Exception {
        UUID id = UUID.randomUUID();

        MvcResult resultado = enviar(id, servicio(), cuerpo("Turista", "Desactivado", 4));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        Map<String, Object> guardada = fila(id);
        assertThat(guardada).containsEntry("rol", "Turista").containsEntry("estado", "Desactivado")
                .containsEntry("version", 4L);
        assertThat(guardada.get("actualizado_en")).isNotNull();
    }

    /**
     * RF45 (DEC-107, DEC-110): el evento de una cuenta de Administrador desconocida crea la fila; con ella, su token pasa
     * la validación de sesión (y la autorización por rol responde 403 en /mias). Al revocar, el token anterior es 401.
     */
    @Test
    void elEventoDeUnAdministradorDesconocidoCreaLaFilaYElDeLaRevocacionInvalidaElTokenAnterior() throws Exception {
        UUID id = UUID.randomUUID();
        assertThat(misReservas(TokensDePrueba.usuario(id, "Administrador", 2)).getResponse().getStatus())
                .isEqualTo(401);

        assertThat(enviar(id, servicio(), cuerpo("Administrador", "Activo", 2)).getResponse().getStatus())
                .isEqualTo(204);
        assertThat(fila(id)).containsEntry("rol", "Administrador").containsEntry("version", 2L);
        assertThat(misReservas(TokensDePrueba.usuario(id, "Administrador", 2)).getResponse().getStatus())
                .isEqualTo(403);

        assertThat(enviar(id, servicio(), cuerpo("Turista", "Activo", 3)).getResponse().getStatus()).isEqualTo(204);
        assertThat(fila(id)).containsEntry("rol", "Turista").containsEntry("version", 3L);
        MvcResult viejo = misReservas(TokensDePrueba.usuario(id, "Administrador", 2));
        assertThat(viejo.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(texto(viejo), "$.codigo")).isEqualTo("SESION_INVALIDADA");
        assertThat(misReservas(TokensDePrueba.turista(id, 3)).getResponse().getStatus()).isEqualTo(200);
        jdbc.update("DELETE FROM cuenta WHERE id_usuario = ?", id);
    }

    private MvcResult misReservas(String token) throws Exception {
        return mvc.perform(get("/api/v1/reservas/mias").header(HttpHeaders.AUTHORIZATION,
                TokensDePrueba.bearer(token))).andReturn();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Pendiente", "Activo", "Rechazado", "Inactivo", "Desactivado"})
    void aceptaCadaEstadoDerivadoConUn204(String estado) throws Exception {
        UUID id = UUID.randomUUID();

        MvcResult resultado = enviar(id, servicio(), cuerpo("Prestador", estado, 1));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        assertThat(fila(id)).containsEntry("estado", estado);
    }

    @Test
    void unaVersionMenorOIgualSeIgnoraYResponde204() throws Exception {
        UUID id = UUID.randomUUID();
        enviar(id, servicio(), cuerpo("Turista", "Desactivado", 5));

        MvcResult igual = enviar(id, servicio(), cuerpo("Turista", "Activo", 5));
        MvcResult menor = enviar(id, servicio(), cuerpo("Turista", "Activo", 2));

        assertThat(igual.getResponse().getStatus()).isEqualTo(204);
        assertThat(menor.getResponse().getStatus()).isEqualTo(204);
        assertThat(fila(id)).containsEntry("estado", "Desactivado").containsEntry("version", 5L);

        assertThat(enviar(id, servicio(), cuerpo("Turista", "Activo", 6)).getResponse().getStatus()).isEqualTo(204);
        assertThat(fila(id)).containsEntry("estado", "Activo").containsEntry("version", 6L);
    }

    @Test
    void unCuerpoInvalidoResponde400ConUnCodigoPorCampo() throws Exception {
        UUID id = UUID.randomUUID();

        MvcResult resultado = enviar(id, servicio(),
                "{\"rol\":\"Visitante\",\"estadoCuenta\":\"Suspendido\",\"version\":0}");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        String json = texto(resultado);
        assertThat(JsonPath.<String>read(json, "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        assertThat(JsonPath.<String>read(json, "$.campos.rol")).isEqualTo("NO_PERMITIDO");
        assertThat(JsonPath.<String>read(json, "$.campos.estadoCuenta")).isEqualTo("NO_PERMITIDO");
        assertThat(JsonPath.<String>read(json, "$.campos.version")).isEqualTo("VALOR_MINIMO");

        MvcResult vacio = enviar(id, servicio(), "{}");
        assertThat(JsonPath.<String>read(texto(vacio), "$.campos.version")).isEqualTo("REQUERIDO");
        assertThat(fila(id)).isNull();
    }

    @Test
    void laTablaRechazaUnEstadoQueNoEsDeLosCinco() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO cuenta (id_usuario, rol, estado, version, actualizado_en)"
                + " VALUES (?, 'Turista', 'Suspendido', 1, now())", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ck_cuenta_estado");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO cuenta (id_usuario, rol, estado, version, actualizado_en)"
                + " VALUES (?, 'Turista', 'Activo', 0, now())", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ck_cuenta_version");
    }

    /**
     * N8 (ADR-008), defensa del servicio: aunque el gateway ya la rechaza, una ruta no normalizada que llegue directo
     * la rechaza el StrictHttpFirewall de Spring Security antes de cualquier cadena de seguridad, incluso con un JWT de
     * servicio válido.
     */
    @Test
    void unaRutaNoNormalizadaHaciaLosEndpointsInternosEs400() throws Exception {
        UUID id = UUID.randomUUID();

        MvcResult resultado = mvc.perform(put("/api/v1/reservas/../../internal/v1/cuentas/" + id)
                .header(HttpHeaders.AUTHORIZATION, servicio()).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("Turista", "Desactivado", 1))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(fila(id)).isNull();
    }

    private static String texto(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
