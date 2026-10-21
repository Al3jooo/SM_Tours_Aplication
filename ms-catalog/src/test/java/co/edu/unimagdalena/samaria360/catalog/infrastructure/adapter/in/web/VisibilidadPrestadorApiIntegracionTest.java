package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.TokensDePrueba;
import com.jayway.jsonpath.JsonPath;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * RF22 de punta a punta: cuando ms-auth entrega «Inactivo» para un prestador, sus servicios salen de la búsqueda
 * pública y el detalle responde 404; con «Activo» vuelven. El prestador de la prueba es propio (no toca la semilla).
 */
@PruebaIntegracion
class VisibilidadPrestadorApiIntegracionTest {

    private static final String NOMBRE = "Cabañas de la prueba de visibilidad";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    private final UUID prestador = UUID.randomUUID();
    private ObjectId idServicio;

    @BeforeEach
    void crearServicioDelPrestador() {
        Document modelo = mongo.getCollection("servicios").find(new Document("nombreComercial", "Cabañas El Roble"))
                .first();
        assertThat(modelo).isNotNull();
        Document copia = new Document(modelo);
        idServicio = new ObjectId();
        copia.put("_id", idServicio);
        copia.put("nombreComercial", NOMBRE);
        copia.put("prestadorId", prestador.toString());
        copia.put("prestadorActivo", true);
        copia.put("estadoPublicacion", "Publicado");
        mongo.getCollection("servicios").insertOne(copia);
    }

    @AfterEach
    void limpiar() {
        mongo.getCollection("servicios").deleteMany(new Document("prestadorId", prestador.toString()));
        mongo.getCollection("cuentas").deleteOne(new Document("_id", prestador.toString()));
    }

    @Test
    void inactivarAlPrestadorOcultaSusServiciosYReactivarloLosDevuelve() throws Exception {
        assertThat(nombresEnLaOferta()).contains(NOMBRE);
        assertThat(estadoDelDetalle()).isEqualTo(200);

        assertThat(enviar("Inactivo", 1)).isEqualTo(204);

        assertThat(nombresEnLaOferta()).doesNotContain(NOMBRE);
        assertThat(estadoDelDetalle()).isEqualTo(404);
        assertThat(mongo.getCollection("servicios").find(new Document("_id", idServicio)).first()
                .getBoolean("prestadorActivo")).isFalse();

        assertThat(enviar("Activo", 2)).isEqualTo(204);

        assertThat(nombresEnLaOferta()).contains(NOMBRE);
        assertThat(estadoDelDetalle()).isEqualTo(200);
    }

    @Test
    void unEventoViejoLlegadoTardeNoVuelveAMostrarLosServicios() throws Exception {
        enviar("Inactivo", 3);

        assertThat(enviar("Activo", 2)).isEqualTo(204);

        assertThat(nombresEnLaOferta()).doesNotContain(NOMBRE);
        assertThat(estadoDelDetalle()).isEqualTo(404);
    }

    @Test
    void unPrestadorPendienteORechazadoNoTieneServiciosVisibles() throws Exception {
        assertThat(enviar("Rechazado", 1)).isEqualTo(204);

        assertThat(nombresEnLaOferta()).doesNotContain(NOMBRE);
    }

    @Test
    void laOfertaPublicaSigueRespondiendoSinToken() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/v1/catalogo/oferta?tamano=10")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    private int enviar(String estado, long version) throws Exception {
        return mvc.perform(put("/internal/v1/cuentas/" + prestador)
                .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(TokensDePrueba.servicio("ms-catalog")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rol\":\"Prestador\",\"estadoCuenta\":\"" + estado + "\",\"version\":" + version + "}"))
                .andReturn().getResponse().getStatus();
    }

    private List<String> nombresEnLaOferta() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/v1/catalogo/oferta").param("texto", NOMBRE)).andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.elementos[*].nombre");
    }

    private int estadoDelDetalle() throws Exception {
        return mvc.perform(get("/api/v1/catalogo/servicios/" + idServicio.toHexString())).andReturn().getResponse()
                .getStatus();
    }
}
