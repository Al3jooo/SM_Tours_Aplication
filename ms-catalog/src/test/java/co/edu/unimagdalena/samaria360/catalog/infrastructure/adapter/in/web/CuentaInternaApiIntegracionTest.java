package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.TokensDePrueba;
import com.jayway.jsonpath.JsonPath;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * {@code PUT /internal/v1/cuentas/{id}} (ADR-008) contra MongoDB: solo el JWT de servicio con {@code aud = ms-catalog},
 * *upsert* idempotente por versión y validación del cuerpo.
 */
@PruebaIntegracion
class CuentaInternaApiIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    private static String ruta(UUID id) {
        return "/internal/v1/cuentas/" + id;
    }

    private static String cuerpo(String rol, String estado, long version) {
        return "{\"rol\":\"" + rol + "\",\"estadoCuenta\":\"" + estado + "\",\"version\":" + version + "}";
    }

    private MvcResult enviar(UUID id, String autorizacion, String json) throws Exception {
        var solicitud = put(ruta(id)).contentType(MediaType.APPLICATION_JSON).content(json);
        if (autorizacion != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, autorizacion);
        }
        return mvc.perform(solicitud).andReturn();
    }

    private static String servicio() {
        return TokensDePrueba.bearer(TokensDePrueba.servicio("ms-catalog"));
    }

    private Document proyeccion(UUID id) {
        return mongo.getCollection("cuentas").find(new Document("_id", id.toString())).first();
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
        String turista = TokensDePrueba.bearer(TokensDePrueba.usuario(UUID.randomUUID(), "Turista", 0));
        String administrador = TokensDePrueba.bearer(TokensDePrueba.usuario(UUID.randomUUID(), "Administrador", 0));

        for (String token : new String[]{turista, administrador}) {
            MvcResult resultado = enviar(id, token, cuerpo("Turista", "Desactivado", 1));
            assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
            assertThat(JsonPath.<String>read(texto(resultado), "$.codigo")).isEqualTo("ACCESO_DENEGADO");
        }
        assertThat(proyeccion(id)).isNull();
    }

    @Test
    void unTokenDeServicioConOtraAudienciaResponde401() throws Exception {
        UUID id = UUID.randomUUID();

        MvcResult resultado = enviar(id, TokensDePrueba.bearer(TokensDePrueba.servicio("ms-booking")),
                cuerpo("Turista", "Desactivado", 1));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(texto(resultado), "$.codigo")).isEqualTo("NO_AUTENTICADO");
        assertThat(proyeccion(id)).isNull();
    }

    @Test
    void unTokenDeServicioValidoGuardaLaProyeccion() throws Exception {
        UUID id = UUID.randomUUID();

        MvcResult resultado = enviar(id, servicio(), cuerpo("Turista", "Desactivado", 4));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        Document guardada = proyeccion(id);
        assertThat(guardada.getString("rol")).isEqualTo("Turista");
        assertThat(guardada.getString("estadoCuenta")).isEqualTo("Desactivado");
        assertThat(guardada.get("version")).isEqualTo(4L);
        assertThat(guardada.getDate("actualizadoEn")).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Pendiente", "Activo", "Rechazado", "Inactivo", "Desactivado"})
    void aceptaCadaEstadoDerivadoConUn204(String estado) throws Exception {
        UUID id = UUID.randomUUID();

        MvcResult resultado = enviar(id, servicio(), cuerpo("Prestador", estado, 1));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        assertThat(proyeccion(id).getString("estadoCuenta")).isEqualTo(estado);
    }

    @Test
    void unaVersionMenorOIgualSeIgnoraYResponde204() throws Exception {
        UUID id = UUID.randomUUID();
        enviar(id, servicio(), cuerpo("Turista", "Desactivado", 5));

        MvcResult igual = enviar(id, servicio(), cuerpo("Turista", "Activo", 5));
        MvcResult menor = enviar(id, servicio(), cuerpo("Turista", "Activo", 2));

        assertThat(igual.getResponse().getStatus()).isEqualTo(204);
        assertThat(menor.getResponse().getStatus()).isEqualTo(204);
        Document guardada = proyeccion(id);
        assertThat(guardada.getString("estadoCuenta")).isEqualTo("Desactivado");
        assertThat(guardada.get("version")).isEqualTo(5L);

        assertThat(enviar(id, servicio(), cuerpo("Turista", "Activo", 6)).getResponse().getStatus()).isEqualTo(204);
        assertThat(proyeccion(id).getString("estadoCuenta")).isEqualTo("Activo");
    }

    @Test
    void uncuerpoInvalidoResponde400ConUnCodigoPorCampo() throws Exception {
        UUID id = UUID.randomUUID();

        MvcResult resultado = enviar(id, servicio(), "{\"rol\":\"Visitante\",\"estadoCuenta\":\"Suspendido\",\"version\":0}");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        String json = texto(resultado);
        assertThat(JsonPath.<String>read(json, "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        assertThat(JsonPath.<String>read(json, "$.campos.rol")).isEqualTo("NO_PERMITIDO");
        assertThat(JsonPath.<String>read(json, "$.campos.estadoCuenta")).isEqualTo("NO_PERMITIDO");
        assertThat(JsonPath.<String>read(json, "$.campos.version")).isEqualTo("VALOR_MINIMO");

        MvcResult vacio = enviar(id, servicio(), "{}");
        assertThat(JsonPath.<String>read(texto(vacio), "$.campos.version")).isEqualTo("REQUERIDO");
        assertThat(proyeccion(id)).isNull();
    }

    @Test
    void unIdQueNoEsUnUuidResponde400() throws Exception {
        MvcResult resultado = mvc.perform(put("/internal/v1/cuentas/no-es-un-uuid")
                .header(HttpHeaders.AUTHORIZATION, servicio()).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("Turista", "Activo", 1))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    /**
     * RF45 (DEC-107, DEC-110): el evento de una cuenta desconocida crea la fila; con ella, el token del Administrador
     * vale. Al revocar, el evento con rol Turista y la versión nueva invalida el token anterior.
     */
    @Test
    void elEventoDeUnAdministradorDesconocidoCreaLaFilaYElDeLaRevocacionInvalidaElTokenAnterior() throws Exception {
        UUID id = UUID.randomUUID();
        assertThat(oferta(TokensDePrueba.usuario(id, "Administrador", 2)).getResponse().getStatus()).isEqualTo(401);

        assertThat(enviar(id, servicio(), cuerpo("Administrador", "Activo", 2)).getResponse().getStatus())
                .isEqualTo(204);
        assertThat(proyeccion(id).getString("rol")).isEqualTo("Administrador");
        assertThat(oferta(TokensDePrueba.usuario(id, "Administrador", 2)).getResponse().getStatus()).isEqualTo(200);

        assertThat(enviar(id, servicio(), cuerpo("Turista", "Activo", 3)).getResponse().getStatus()).isEqualTo(204);
        Document revocada = proyeccion(id);
        assertThat(revocada.getString("rol")).isEqualTo("Turista");
        assertThat(revocada.get("version")).isEqualTo(3L);
        MvcResult viejo = oferta(TokensDePrueba.usuario(id, "Administrador", 2));
        assertThat(viejo.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(texto(viejo), "$.codigo")).isEqualTo("SESION_INVALIDADA");
        assertThat(oferta(TokensDePrueba.usuario(id, "Turista", 2)).getResponse().getStatus()).isEqualTo(401);
        assertThat(oferta(TokensDePrueba.usuario(id, "Turista", 3)).getResponse().getStatus()).isEqualTo(200);
        mongo.getCollection("cuentas").deleteOne(new Document("_id", id.toString()));
    }

    private MvcResult oferta(String token) throws Exception {
        return mvc.perform(get("/api/v1/catalogo/oferta").header(HttpHeaders.AUTHORIZATION,
                TokensDePrueba.bearer(token))).andReturn();
    }

    private static String texto(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
