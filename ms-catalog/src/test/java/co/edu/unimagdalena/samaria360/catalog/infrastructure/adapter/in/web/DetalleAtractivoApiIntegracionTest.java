package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * CU «Consultar detalle de atractivo» (DEC-217) con la semilla: público, sin token, visible en cualquier estado
 * operativo (DEC-87, decisión 5 de la búsqueda) y sin el Gestor ni la versión.
 */
@PruebaIntegracion
class DetalleAtractivoApiIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    @Test
    void laFichaPublicaTraeLosDatosDeRf06Rf07Rf24Rf27Rf33SinElGestorNiLaVersion() throws Exception {
        String json = cuerpo(detalle(idDe("Quinta de San Pedro Alejandrino")));

        assertThat(JsonPath.<String>read(json, "$.nombre")).isEqualTo("Quinta de San Pedro Alejandrino");
        assertThat(JsonPath.<String>read(json, "$.categoria")).isEqualTo("Cultura");
        assertThat(JsonPath.<Integer>read(json, "$.limiteCapacidadCarga")).isEqualTo(500);
        assertThat(JsonPath.<String>read(json, "$.estadoOperativo")).isEqualTo("Abierto");
        assertThat(JsonPath.<String>read(json, "$.normasAmbientales")).isEqualTo("No arranques plantas del jardín botánico.");
        assertThat(JsonPath.<List<String>>read(json, "$.condicionesAccesibilidad"))
                .containsExactly("MovilidadReducida", "Visual");
        assertThat(JsonPath.<List<String>>read(json, "$.imagenes"))
                .containsExactly("/imagenes/catalogo/quinta-san-pedro-alejandrino.webp");
        assertThat(JsonPath.<Double>read(json, "$.coordenadas.longitud")).isEqualTo(-74.188);
        assertThat(JsonPath.<Object>read(json, "$.horarioAtencion")).as("la semilla no lo trae").isNull();
        assertThat(json).doesNotContain("gestorId", "version", "fechaRegistro");
    }

    @Test
    void unAtractivoEnMantenimientoTambienSeVe() throws Exception {
        MvcResult resultado = detalle(idDe("Mirador de Taganga"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.estadoOperativo")).isEqualTo("En_mantenimiento");
    }

    @Test
    void unIdInvalidoOInexistenteEs404AtractivoNoEncontrado() throws Exception {
        for (String id : List.of("abc", new ObjectId().toHexString(), idDeServicio())) {
            MvcResult resultado = detalle(id);
            assertThat(resultado.getResponse().getStatus()).as(id).isEqualTo(404);
            assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo")).isEqualTo("ATRACTIVO_NO_ENCONTRADO");
        }
    }

    private MvcResult detalle(String id) throws Exception {
        return mvc.perform(get("/api/v1/catalogo/atractivos/" + id)).andReturn();
    }

    private String idDe(String nombre) {
        return mongo.getCollection("atractivos").find(Filters.eq("nombre", nombre)).first()
                .getObjectId("_id").toHexString();
    }

    /** Un servicio no es un atractivo aunque el id exista en otra colección. */
    private String idDeServicio() {
        return mongo.getCollection("servicios").find().first().getObjectId("_id").toHexString();
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
