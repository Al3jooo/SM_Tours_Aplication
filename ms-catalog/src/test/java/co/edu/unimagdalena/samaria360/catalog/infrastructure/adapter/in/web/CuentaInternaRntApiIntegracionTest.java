package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.TokensDePrueba;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * El RNT del evento {@code CuentaActualizada} en la proyección {@code cuentas} (DEC-126, DEC-146): se guarda solo para
 * un prestador, sigue el upsert idempotente por versión y no queda un valor viejo si un evento llega sin él.
 */
@PruebaIntegracion
class CuentaInternaRntApiIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    private final UUID prestador = UUID.randomUUID();
    private final UUID turista = UUID.randomUUID();

    @AfterEach
    void limpiar() {
        mongo.getCollection("cuentas").deleteMany(Filters.in("_id", prestador.toString(), turista.toString()));
    }

    @Test
    void elRntDeUnPrestadorSeGuardaYUnaVersionViejaNoLoCambia() throws Exception {
        assertThat(enviar(prestador, "{\"rol\":\"Prestador\",\"estadoCuenta\":\"Activo\",\"version\":2,"
                + "\"rnt\":\" RNT-12345 \"}").getResponse().getStatus()).isEqualTo(204);
        assertThat(cuenta(prestador).getString("rnt")).isEqualTo("RNT-12345");

        enviar(prestador, "{\"rol\":\"Prestador\",\"estadoCuenta\":\"Inactivo\",\"version\":1,\"rnt\":\"OTRO\"}");

        assertThat(cuenta(prestador).getString("rnt")).isEqualTo("RNT-12345");
        assertThat(cuenta(prestador).getString("estadoCuenta")).isEqualTo("Activo");
    }

    @Test
    void unEventoSinRntNoDejaElValorAnterior() throws Exception {
        enviar(prestador, "{\"rol\":\"Prestador\",\"estadoCuenta\":\"Activo\",\"version\":2,\"rnt\":\"RNT-1\"}");

        enviar(prestador, "{\"rol\":\"Prestador\",\"estadoCuenta\":\"Activo\",\"version\":3}");

        assertThat(cuenta(prestador).containsKey("rnt")).isFalse();
    }

    @Test
    void elRntDeOtroRolSeIgnora() throws Exception {
        assertThat(enviar(turista, "{\"rol\":\"Turista\",\"estadoCuenta\":\"Activo\",\"version\":1,"
                + "\"rnt\":\"RNT-1\"}").getResponse().getStatus()).isEqualTo(204);

        assertThat(cuenta(turista).keySet()).containsExactlyInAnyOrder("_id", "rol", "estadoCuenta", "version",
                "actualizadoEn");
    }

    @Test
    void unRntDeMasDe50CaracteresEs400() throws Exception {
        MvcResult resultado = enviar(prestador, "{\"rol\":\"Prestador\",\"estadoCuenta\":\"Activo\",\"version\":1,"
                + "\"rnt\":\"" + "R".repeat(51) + "\"}");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<Map<String, String>>read(resultado.getResponse().getContentAsString(
                StandardCharsets.UTF_8), "$.campos")).containsOnly(Map.entry("rnt", "LONGITUD_MAXIMA"));
        assertThat(mongo.getCollection("cuentas").countDocuments(Filters.eq("_id", prestador.toString()))).isZero();
    }

    private MvcResult enviar(UUID id, String cuerpo) throws Exception {
        return mvc.perform(put("/internal/v1/cuentas/" + id)
                .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(TokensDePrueba.servicio("ms-catalog")))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
    }

    private Document cuenta(UUID id) {
        List<Document> encontradas = mongo.getCollection("cuentas").find(Filters.eq("_id", id.toString()))
                .into(new java.util.ArrayList<>());
        assertThat(encontradas).hasSize(1);
        return encontradas.get(0);
    }
}
