package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * CU «Consultar agenda de eventos» (RF25) contra MongoDB: pública (DEC-239), solo los vigentes en Bogotá y por fecha de
 * inicio ascendente, de 10 en 10 (DEC-215). Los eventos se escriben directamente en la colección, con un Gestor propio.
 */
@PruebaIntegracion
class AgendaApiIntegracionTest {

    private static final String RUTA = "/api/v1/catalogo/eventos";
    private static final String GESTOR = "00000000-0000-4000-8000-0000000000a2";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private RelojFijo reloj;

    @BeforeEach
    void preparar() {
        eventos().deleteMany(Filters.eq("gestorId", GESTOR));
        reloj.ajustar(Instant.parse("2026-11-05T15:00:00Z"));
    }

    @AfterEach
    void limpiar() {
        eventos().deleteMany(Filters.eq("gestorId", GESTOR));
    }

    @Test
    void sinTokenTraeSoloLosVigentesPorFechaDeInicio() throws Exception {
        guardar("Diciembre", "2026-12-01", "2026-12-02");
        guardar("En curso", "2026-11-01", "2026-11-30");
        guardar("Termina hoy", "2026-11-05", "2026-11-05");
        guardar("Terminó ayer", "2026-11-01", "2026-11-04");

        MvcResult resultado = mvc.perform(get(RUTA)).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        List<String> nombres = leer(resultado, "$.elementos[*].nombre");
        assertThat(nombres).containsExactly("En curso", "Termina hoy", "Diciembre");
        assertThat(((Number) leer(resultado, "$.totalElementos")).longValue()).isEqualTo(3);
        Map<String, Object> primero = leer(resultado, "$.elementos[0]");
        assertThat(primero).containsOnlyKeys("id", "nombre", "fechaInicio", "fechaFin", "ubicacion", "descripcion")
                .containsEntry("fechaInicio", "2026-11-01").containsEntry("fechaFin", "2026-11-30");
    }

    /** El borde de medianoche de Bogotá: a las 00:00 del 6 (05:00Z), «Termina hoy» ya no está. */
    @Test
    void unEventoSaleALaMedianocheDeBogota() throws Exception {
        guardar("Termina el 5", "2026-11-05", "2026-11-05");

        reloj.ajustar(Instant.parse("2026-11-06T04:59:59Z"));
        List<String> antes = leer(mvc.perform(get(RUTA)).andReturn(), "$.elementos[*].nombre");
        reloj.ajustar(Instant.parse("2026-11-06T05:00:00Z"));
        List<String> despues = leer(mvc.perform(get(RUTA)).andReturn(), "$.elementos[*].nombre");

        assertThat(antes).containsExactly("Termina el 5");
        assertThat(despues).isEmpty();
    }

    @Test
    void paginaDeDiezEnDiez() throws Exception {
        for (int dia = 10; dia < 22; dia++) {
            guardar("Día " + dia, "2026-11-" + dia, "2026-11-" + dia);
        }

        MvcResult segunda = mvc.perform(get(RUTA).param("pagina", "1")).andReturn();

        List<String> nombres = leer(segunda, "$.elementos[*].nombre");
        assertThat(nombres).containsExactly("Día 20", "Día 21");
        assertThat(((Number) leer(segunda, "$.totalPaginas")).intValue()).isEqualTo(2);
    }

    @Test
    void unaPaginaInvalidaEs400YUnBearerInvalidoEs401() throws Exception {
        MvcResult pagina = mvc.perform(get(RUTA).param("pagina", "-1")).andReturn();
        MvcResult bearer = mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, "Bearer no-es-un-jwt")).andReturn();

        assertThat(pagina.getResponse().getStatus()).isEqualTo(400);
        assertThat(leer(pagina, "$.campos.pagina").toString()).isEqualTo("FORMATO");
        assertThat(bearer.getResponse().getStatus()).isEqualTo(401);
    }

    private MongoCollection<Document> eventos() {
        return mongo.getCollection("eventos");
    }

    /** Un evento como lo guarda el adaptador: los días a las 00:00 de Bogotá (05:00Z). */
    private void guardar(String nombre, String inicio, String fin) {
        eventos().insertOne(new Document("_id", new ObjectId()).append("gestorId", GESTOR).append("nombre", nombre)
                .append("fechaInicio", Date.from(Instant.parse(inicio + "T05:00:00Z")))
                .append("fechaFin", Date.from(Instant.parse(fin + "T05:00:00Z")))
                .append("ubicacion", "Santa Marta").append("descripcion", "Descripción")
                .append("fechaRegistro", new Date()));
    }

    private static <T> T leer(MvcResult resultado, String ruta) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), ruta);
    }
}
