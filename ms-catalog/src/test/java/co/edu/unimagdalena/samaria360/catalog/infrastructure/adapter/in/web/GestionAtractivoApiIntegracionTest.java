package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.MongoDatabase;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.con;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.datos;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.tokenGestor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Panel «Atractivos» (WF-24), CU «Actualizar atractivo» y «Cambiar estado operativo» contra MongoDB (RF07, RF24, RF27,
 * RF33, RF58; DEC-206, DEC-207, DEC-210). Cada prueba edita un atractivo propio: los de la semilla no cambian.
 */
@PruebaIntegracion
class GestionAtractivoApiIntegracionTest {

    private static final Instant AHORA = Instant.parse("2026-10-30T16:00:00.500Z");
    private static final String RUTA = SolicitudesAtractivo.RUTA;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private RelojFijo reloj;

    private final UUID gestor = UUID.randomUUID();
    /** Quien lo registró: otro Gestor (DEC-206, cualquier Gestor edita cualquier atractivo). */
    private final UUID registrador = UUID.randomUUID();
    private String id;

    @BeforeEach
    void preparar() {
        reloj.ajustar(AHORA);
        SolicitudesAtractivo.filaDeCuenta(base(), gestor, "Gestor", 1);
        ObjectId objectId = new ObjectId();
        id = objectId.toHexString();
        base().getCollection("atractivos").insertOne(new Document("_id", objectId)
                .append("gestorId", registrador.toString())
                .append("nombre", "Atractivo de gestión " + id)
                .append("coordenadas", new Document("type", "Point").append("coordinates", List.of(-74.1, 11.2)))
                .append("descripcion", "Antes")
                .append("categoria", "Naturaleza")
                .append("imagenes", List.of("/imagenes/catalogo/cascada-el-salto.webp"))
                .append("limiteCapacidadCarga", 50)
                .append("estadoOperativo", "Abierto")
                .append("normasAmbientales", "Normas de antes")
                .append("condicionesAccesibilidad", List.of())
                // Posterior a la semilla, cuya fecha sale de su _id (M012): así encabeza el panel.
                .append("fechaRegistro", Date.from(Instant.parse("2099-01-01T00:00:00Z")))
                .append("version", 0));
    }

    @AfterEach
    void limpiar() {
        SolicitudesAtractivo.limpiar(base(), gestor);
        SolicitudesAtractivo.limpiar(base(), registrador);
    }

    @Test
    void elPanelListaTodosLosAtractivosDelMasRecienteAlMasAntiguo() throws Exception {
        String json = cuerpo(mvc.perform(conToken(get(RUTA))).andReturn());

        assertThat(JsonPath.<Integer>read(json, "$.tamano")).isEqualTo(10);
        assertThat(JsonPath.<Integer>read(json, "$.totalElementos")).isGreaterThanOrEqualTo(8);
        assertThat(JsonPath.<String>read(json, "$.elementos[0].id")).isEqualTo(id);
        assertThat(JsonPath.<Integer>read(json, "$.elementos[0].version")).isZero();
        assertThat(JsonPath.<String>read(json, "$.elementos[0].imagen"))
                .isEqualTo("/imagenes/catalogo/cascada-el-salto.webp");
        assertThat(cuerpo(mvc.perform(conToken(get(RUTA).param("pagina", "abc"))).andReturn()))
                .contains("\"pagina\":\"FORMATO\"");
    }

    @Test
    void elDetalleDeGestionTraeLaVersionYQuienLoRegistro() throws Exception {
        String json = cuerpo(mvc.perform(conToken(get(RUTA + "/" + id))).andReturn());

        assertThat(JsonPath.<String>read(json, "$.gestorId")).isEqualTo(registrador.toString());
        assertThat(JsonPath.<Integer>read(json, "$.version")).isZero();
        assertThat(mvc.perform(conToken(get(RUTA + "/" + new ObjectId()))).andReturn().getResponse().getStatus())
                .isEqualTo(404);
    }

    @Test
    void editaLosDatosQuitaLosOpcionalesVaciosYSubeLaVersion() throws Exception {
        Map<String, Object> cuerpo = con(datos("Atractivo editado " + id), d -> {
            d.put("version", 0);
            d.put("normasAmbientales", " ");
            d.put("estadoOperativo", "En_mantenimiento");
        });

        MvcResult resultado = mvc.perform(conToken(put(RUTA + "/" + id)).contentType(MediaType.APPLICATION_JSON)
                .content(SolicitudesServicio.json(cuerpo))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(cuerpo(resultado), "$.version")).isEqualTo(1);
        Document guardado = guardado();
        assertThat(guardado.getString("nombre")).isEqualTo("Atractivo editado " + id);
        assertThat(guardado.getString("horarioAtencion")).isEqualTo("Martes a domingo, 8:00 a 16:00");
        assertThat(guardado.containsKey("normasAmbientales")).isFalse();
        assertThat(guardado.getString("estadoOperativo")).as("el estado no se edita aquí").isEqualTo("Abierto");
        assertThat(guardado.getList("imagenes", String.class)).containsExactly("/imagenes/catalogo/cascada-el-salto.webp");
        assertThat(guardado.getString("gestorId")).isEqualTo(registrador.toString());
        assertThat(guardado.getDate("fechaActualizacion")).isEqualTo(Date.from(AHORA));
        assertThat(guardado.get("version")).isEqualTo(1);
    }

    @Test
    void cambiaElEstadoYUnaVersionViejaEsAtractivoModificado() throws Exception {
        MvcResult cambio = estado("Cerrado_temporalmente", 0);
        MvcResult viejo = estado("En_mantenimiento", 0);

        assertThat(cambio.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<String>read(cuerpo(cambio), "$.estadoOperativo")).isEqualTo("Cerrado_temporalmente");
        assertThat(viejo.getResponse().getStatus()).isEqualTo(409);
        assertThat(JsonPath.<String>read(cuerpo(viejo), "$.codigo")).isEqualTo("ATRACTIVO_MODIFICADO");
        assertThat(JsonPath.<Integer>read(cuerpo(viejo), "$.versionActual")).isEqualTo(1);
        assertThat(guardado().getString("estadoOperativo")).isEqualTo("Cerrado_temporalmente");

        String publico = cuerpo(mvc.perform(get("/api/v1/catalogo/atractivos/" + id)).andReturn());
        assertThat(JsonPath.<String>read(publico, "$.estadoOperativo")).isEqualTo("Cerrado_temporalmente");
    }

    @Test
    void elMismoEstadoOUnoDesconocidoSon400() throws Exception {
        MvcResult mismo = estado("Abierto", 0);
        MvcResult desconocido = estado("Cerrado", 0);

        assertThat(mismo.getResponse().getStatus()).isEqualTo(400);
        assertThat(cuerpo(mismo)).contains("\"estadoNuevo\":\"NO_PERMITIDO\"");
        assertThat(cuerpo(desconocido)).contains("\"estadoNuevo\":\"NO_PERMITIDO\"");
        assertThat(guardado().get("version")).isEqualTo(0);
    }

    @Test
    void unIdQueNoEsUnObjectIdOInexistenteEs404AtractivoNoEncontrado() throws Exception {
        for (String otro : List.of("abc", new ObjectId().toHexString())) {
            MvcResult resultado = mvc.perform(conToken(post(RUTA + "/" + otro + "/estado"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"estadoNuevo\":\"Abierto\",\"version\":0}")).andReturn();
            assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
            assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo")).isEqualTo("ATRACTIVO_NO_ENCONTRADO");
        }
    }

    // ---- auxiliares ------------------------------------------------------------------------------------------

    private MvcResult estado(String estadoNuevo, int version) throws Exception {
        return mvc.perform(conToken(post(RUTA + "/" + id + "/estado")).contentType(MediaType.APPLICATION_JSON)
                .content(SolicitudesServicio.json(Map.of("estadoNuevo", estadoNuevo, "version", version))))
                .andReturn();
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder solicitud) {
        return solicitud.header(HttpHeaders.AUTHORIZATION, tokenGestor(gestor, 1));
    }

    private Document guardado() {
        return base().getCollection("atractivos").find(Filters.eq("_id", new ObjectId(id))).first();
    }

    private MongoDatabase base() {
        return mongo.getDb();
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
