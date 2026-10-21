package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.booking.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.booking.infrastructure.TokensDePrueba;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * DEC-29: ms-booking compara el claim {@code ver} con la proyección {@code cuenta}. Reservar, consultar y cancelar siguen
 * siendo los mismos casos de uso; solo el validador del JWT consulta la tabla. Un token de antes de un cambio de estado
 * recibe 401 SESION_INVALIDADA.
 */
@PruebaIntegracion
class SesionVersionIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final UUID turista = UUID.randomUUID();

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM cuenta WHERE id_usuario = ?", turista);
    }

    private void proyeccion(long version) {
        jdbc.update("INSERT INTO cuenta (id_usuario, rol, estado, version, actualizado_en)"
                + " VALUES (?, 'Turista', 'Desactivado', ?, now())", turista, version);
    }

    private MvcResult reservar(String token) throws Exception {
        return mvc.perform(post("/api/v1/reservas").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(token))
                .header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andReturn();
    }

    private MvcResult misReservas(String token) throws Exception {
        return mvc.perform(get("/api/v1/reservas/mias").header(HttpHeaders.AUTHORIZATION,
                TokensDePrueba.bearer(token))).andReturn();
    }

    @Test
    void postReservasConUnVerViejoResponde401SesionInvalidada() throws Exception {
        proyeccion(3);

        MvcResult resultado = reservar(TokensDePrueba.turista(turista, 2));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(texto(resultado), "$.codigo")).isEqualTo("SESION_INVALIDADA");
    }

    @Test
    void tambienConsultarYCancelarExigenUnaSesionVigente() throws Exception {
        proyeccion(3);
        String viejo = TokensDePrueba.turista(turista, 2);

        MvcResult mias = misReservas(viejo);
        MvcResult cancelar = mvc.perform(post("/api/v1/reservas/" + UUID.randomUUID() + "/cancelacion")
                .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(viejo))).andReturn();

        assertThat(mias.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(texto(mias), "$.codigo")).isEqualTo("SESION_INVALIDADA");
        assertThat(cancelar.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void unTokenSinVerYSinFilaEnLaProyeccionSeAcepta() throws Exception {
        MvcResult resultado = misReservas(TokensDePrueba.turista(turista));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void unVerIgualOMayorSeAcepta() throws Exception {
        proyeccion(3);

        assertThat(misReservas(TokensDePrueba.turista(turista, 3)).getResponse().getStatus()).isEqualTo(200);
        assertThat(misReservas(TokensDePrueba.turista(turista, 4)).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void unTokenSinVerCuentaComoCeroYSeRechazaSiLaCuentaYaCambio() throws Exception {
        proyeccion(1);

        MvcResult resultado = misReservas(TokensDePrueba.turista(turista));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(texto(resultado), "$.codigo")).isEqualTo("SESION_INVALIDADA");
    }

    @Test
    void unTokenDeServicioNoSirveEnLaApiPublica() throws Exception {
        MvcResult resultado = misReservas(TokensDePrueba.servicio("ms-booking"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(texto(resultado), "$.codigo")).isEqualTo("NO_AUTENTICADO");
    }

    @Test
    void laDisponibilidadSigueSiendoPublicaSinToken() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/v1/reservas/disponibilidad/resumen")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isNotEqualTo(401);
    }

    private static String texto(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
