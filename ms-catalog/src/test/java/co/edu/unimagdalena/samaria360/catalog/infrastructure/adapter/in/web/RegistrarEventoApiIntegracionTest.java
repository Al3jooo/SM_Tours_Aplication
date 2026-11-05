package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * CU «Registrar evento en agenda» (RF25) y su flujo «Consultar eventos registrados» contra MongoDB, con el validador de
 * M013 (DEC-235 a DEC-238). Cada prueba borra los eventos y la fila de cuenta de su Gestor.
 */
@PruebaIntegracion
class RegistrarEventoApiIntegracionTest {

    private static final String RUTA = "/api/v1/catalogo/gestor/eventos";
    /** 2026-11-05 a las 10:00 en Bogotá. */
    private static final Instant AHORA = Instant.parse("2026-11-05T15:00:00.789Z");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private RelojFijo reloj;

    private final UUID gestor = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        reloj.ajustar(AHORA);
        SolicitudesAtractivo.filaDeCuenta(mongo.getDb(), gestor, "Gestor", 1);
    }

    @AfterEach
    void limpiar() {
        eventos().deleteMany(Filters.eq("gestorId", gestor.toString()));
        mongo.getCollection("cuentas").deleteOne(Filters.eq("_id", gestor.toString()));
    }

    @Test
    void registraElEventoConFechasALaMedianocheDeBogotaYLoDevuelveCon201() throws Exception {
        MvcResult resultado = registrar(cuerpo("Fiesta del Mar", "2026-12-01", "2026-12-03"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        String id = leer(resultado, "$.id");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo(RUTA + "/" + id);
        assertThat(leer(resultado, "$.nombre").toString()).isEqualTo("Fiesta del Mar");
        assertThat(leer(resultado, "$.fechaInicio").toString()).isEqualTo("2026-12-01");
        assertThat(leer(resultado, "$.fechaFin").toString()).isEqualTo("2026-12-03");
        assertThat(leer(resultado, "$.gestorId").toString()).isEqualTo(gestor.toString());
        assertThat(leer(resultado, "$.fechaRegistro").toString()).isEqualTo("2026-11-05T15:00:00.789Z");

        Document guardado = eventos().find(Filters.eq("_id", new ObjectId(id))).first();
        assertThat(guardado.getDate("fechaInicio").toInstant()).isEqualTo(Instant.parse("2026-12-01T05:00:00Z"));
        assertThat(guardado.getDate("fechaFin").toInstant()).isEqualTo(Instant.parse("2026-12-03T05:00:00Z"));
        assertThat(guardado.keySet()).containsExactlyInAnyOrder("_id", "gestorId", "nombre", "fechaInicio", "fechaFin",
                "ubicacion", "descripcion", "fechaRegistro");
    }

    @Test
    void loQueFijaElServidorSeIgnora() throws Exception {
        Map<String, Object> datos = cuerpo("Con extras", "2026-12-01", "2026-12-01");
        datos.put("gestorId", UUID.randomUUID().toString());
        datos.put("fechaRegistro", "2020-01-01T00:00:00Z");
        datos.put("version", 7);

        MvcResult resultado = registrar(datos);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(leer(resultado, "$.gestorId").toString()).isEqualTo(gestor.toString());
        assertThat(leer(resultado, "$.fechaRegistro").toString()).isEqualTo("2026-11-05T15:00:00.789Z");
    }

    @Test
    void losDatosInvalidosResponden400ConCadaCampoYNoSeGuardaNada() throws Exception {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", " ");
        datos.put("fechaInicio", "2026-02-30");
        datos.put("fechaFin", "2026-11-04");
        datos.put("ubicacion", "u".repeat(201));

        MvcResult resultado = registrar(datos);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(leer(resultado, "$.codigo").toString()).isEqualTo("DATOS_INVALIDOS");
        Map<String, String> campos = leer(resultado, "$.campos");
        assertThat(campos).containsExactly(Map.entry("nombre", "REQUERIDO"), Map.entry("fechaInicio", "FORMATO"),
                Map.entry("fechaFin", "NO_PERMITIDO"), Map.entry("ubicacion", "LONGITUD_MAXIMA"),
                Map.entry("descripcion", "REQUERIDO"));
        assertThat(eventos().countDocuments(Filters.eq("gestorId", gestor.toString()))).isZero();
    }

    @Test
    void unaFechaFinAnteriorALaDeInicioEsNoPermitida() throws Exception {
        MvcResult resultado = registrar(cuerpo("Al revés", "2026-12-03", "2026-12-01"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        Map<String, String> campos = leer(resultado, "$.campos");
        assertThat(campos).containsExactly(Map.entry("fechaFin", "NO_PERMITIDO"));
    }

    @Test
    void unCuerpoQueNoEsJsonEs400() throws Exception {
        MvcResult resultado = mvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, token())
                .contentType(MediaType.APPLICATION_JSON).content("no es json")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(leer(resultado, "$.codigo").toString()).isEqualTo("DATOS_INVALIDOS");
    }

    @Test
    void elPanelListaTodosDelMasRecienteAlMasAntiguoDeDiezEnDiez() throws Exception {
        for (int i = 0; i < 11; i++) {
            assertThat(registrar(cuerpo("Evento " + i, "2026-12-01", "2026-12-01")).getResponse().getStatus())
                    .isEqualTo(201);
            reloj.avanzar(Duration.ofMinutes(1));
        }

        MvcResult primera = listar("0");
        MvcResult segunda = listar("1");

        assertThat(primera.getResponse().getStatus()).isEqualTo(200);
        List<String> nombres = leer(primera, "$.elementos[*].nombre");
        assertThat(nombres).hasSize(10).startsWith("Evento 10", "Evento 9");
        assertThat(((Number) leer(primera, "$.totalElementos")).longValue()).isGreaterThanOrEqualTo(11);
        assertThat(((Number) leer(segunda, "$.pagina")).intValue()).isEqualTo(1);
    }

    @Test
    void unaPaginaInvalidaEs400ConCamposPagina() throws Exception {
        for (String pagina : List.of("-1", "abc")) {
            MvcResult resultado = listar(pagina);
            assertThat(resultado.getResponse().getStatus()).as(pagina).isEqualTo(400);
            assertThat(leer(resultado, "$.campos.pagina").toString()).isEqualTo("FORMATO");
        }
    }

    private MongoCollection<Document> eventos() {
        return mongo.getCollection("eventos");
    }

    private String token() {
        return SolicitudesAtractivo.tokenGestor(gestor, 1);
    }

    private static Map<String, Object> cuerpo(String nombre, String inicio, String fin) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", nombre);
        datos.put("fechaInicio", inicio);
        datos.put("fechaFin", fin);
        datos.put("ubicacion", "Bahía de Santa Marta");
        datos.put("descripcion", "Música, gastronomía y cultura del Caribe.");
        return datos;
    }

    private MvcResult registrar(Map<String, Object> datos) throws Exception {
        return mvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, token())
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(datos))).andReturn();
    }

    private MvcResult listar(String pagina) throws Exception {
        return mvc.perform(get(RUTA).param("pagina", pagina).header(HttpHeaders.AUTHORIZATION, token())).andReturn();
    }

    private static <T> T leer(MvcResult resultado, String ruta) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), ruta);
    }
}
