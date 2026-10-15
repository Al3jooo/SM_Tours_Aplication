package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Detalle público de un servicio (WF-06, WF-10, ADR-007) contra MongoDB con la semilla y las migraciones: los
 * alojamientos traen sus unidades (D1) y los servicios no visibles no existen (RF16, RF22).
 */
@PruebaIntegracion
class ServicioApiIntegracionTest {

    private static final String SERVICIOS = "/api/v1/catalogo/servicios/";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    @Test
    void unAlojamientoTraeSusUnidadesYElPrecioDesde() throws Exception {
        String id = idDe("Hotel Casa Tayrona");

        MvcResult resultado = consultar(id);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String json = cuerpo(resultado);
        assertThat(claves(json)).containsExactly("id", "idPrestador", "nombreComercial", "categoria", "descripcion",
                "imagenes", "precioBase", "moneda", "detalleCategoria");
        assertThat((String) JsonPath.read(json, "$.descripcion")).contains("Parque de los Novios");
        assertThat((List<String>) JsonPath.read(json, "$.imagenes")).isNotEmpty()
                .allSatisfy(imagen -> assertThat(imagen).startsWith("/"));
        assertThat((String) JsonPath.read(json, "$.id")).isEqualTo(id);
        assertThat((String) JsonPath.read(json, "$.idPrestador")).isEqualTo("00000000-0000-4000-8000-000000000002");
        assertThat((String) JsonPath.read(json, "$.categoria")).isEqualTo("Alojamiento");
        assertThat(decimal(JsonPath.read(json, "$.precioBase"))).isEqualByComparingTo("150000");
        assertThat((String) JsonPath.read(json, "$.moneda")).isEqualTo("COP");
        assertThat((List<String>) JsonPath.read(json, "$.detalleCategoria.serviciosIncluidos"))
                .containsExactly("WiFi", "Desayuno");

        List<Map<String, Object>> unidades = JsonPath.read(json, "$.detalleCategoria.unidades");
        assertThat(unidades).extracting(unidad -> unidad.get("nombre")).containsExactly("Doble", "Suite familiar");
        assertThat(unidades).allSatisfy(unidad -> {
            assertThat(unidad).containsOnlyKeys("idUnidad", "nombre", "tipoHabitacion", "numHabitaciones",
                    "capacidadMaxHuespedes", "precioNoche");
            assertThat((String) unidad.get("idUnidad")).matches("[0-9a-f]{24}");
        });
        assertThat(unidades.get(1)).containsEntry("tipoHabitacion", "Suite")
                .containsEntry("numHabitaciones", 3).containsEntry("capacidadMaxHuespedes", 4);
        assertThat(decimal(unidades.get(1).get("precioNoche"))).isEqualByComparingTo("260000");
    }

    @Test
    void losIdsDeUnidadSonLosDeLaBase() throws Exception {
        String id = idDe("Hotel Casa Tayrona");
        List<String> enLaBase = new ArrayList<>();
        mongo.getCollection("servicios").find(new Document("_id", new ObjectId(id))).first()
                .get("detalleCategoria", Document.class).getList("unidades", Document.class)
                .forEach(unidad -> enLaBase.add(unidad.getObjectId("idUnidad").toHexString()));

        assertThat((List<String>) JsonPath.read(cuerpo(consultar(id)), "$.detalleCategoria.unidades[*].idUnidad"))
                .containsExactlyElementsOf(enLaBase);
    }

    @Test
    void unTourTraeSuDuracionYSuAforo() throws Exception {
        String json = cuerpo(consultar(idDe("Tour de Café en Minca")));

        assertThat((String) JsonPath.read(json, "$.categoria")).isEqualTo("Tour");
        assertThat(decimal(JsonPath.read(json, "$.precioBase"))).isEqualByComparingTo("80000");
        assertThat(decimal(JsonPath.read(json, "$.detalleCategoria.duracionHoras"))).isEqualByComparingTo("4");
        assertThat((Integer) JsonPath.read(json, "$.detalleCategoria.aforoMaxGrupo")).isEqualTo(12);
        assertThat(JsonPath.<Map<String, Object>>read(json, "$.detalleCategoria"))
                .containsOnlyKeys("puntoEncuentro", "duracionHoras", "nivelDificultad", "aforoMaxGrupo");
    }

    @Test
    void unServicioPendienteODeUnPrestadorInactivoNoExiste() throws Exception {
        String pendiente = mongo.getCollection("servicios").find(new Document("estadoPublicacion", "Pendiente"))
                .first().getObjectId("_id").toHexString();

        assertNoDisponible(consultar(pendiente));
        assertNoDisponible(consultar(idDe("Snorkel en Bahía Concha")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "6ACFF19A69BFCC83461B196D", "000000000000000000000000"})
    void unIdInvalidoOInexistenteNoExiste(String id) throws Exception {
        assertNoDisponible(consultar(id));
    }

    // ------------------------------------------------------------------ auxiliares

    private String idDe(String nombreComercial) {
        return mongo.getCollection("servicios").find(new Document("nombreComercial", nombreComercial)).first()
                .getObjectId("_id").toHexString();
    }

    private MvcResult consultar(String id) throws Exception {
        return mvc.perform(get(SERVICIOS + id)).andReturn();
    }

    private static void assertNoDisponible(MvcResult resultado) throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(resultado.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat((String) JsonPath.read(cuerpo(resultado), "$.codigo")).isEqualTo("OFERTA_NO_DISPONIBLE");
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static List<String> claves(String json) {
        return new ArrayList<>(JsonPath.<Map<String, Object>>read(json, "$").keySet());
    }

    private static BigDecimal decimal(Object valor) {
        return new BigDecimal(valor.toString());
    }
}
