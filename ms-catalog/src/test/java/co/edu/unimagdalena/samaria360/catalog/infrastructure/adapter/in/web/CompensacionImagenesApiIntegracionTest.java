package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.FIRMA_JPEG;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.FIRMA_PNG;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.datos;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.imagen;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.parteImagen;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.registro;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.tokenGestor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * C4 (ADR-009): si el insert del atractivo falla después de guardar las imágenes en GridFS, el caso de uso las borra.
 * El fallo es real: durante la prueba, el validador de {@code atractivos} rechaza cualquier documento; al terminar se
 * restaura el validador v2. Se comprueban {@code imagenes.files} e {@code imagenes.chunks}.
 */
@PruebaIntegracion
class CompensacionImagenesApiIntegracionTest {

    private static final Document RECHAZA_TODO =
            new Document("$jsonSchema", new Document("required", List.of("campoQueNoExiste")));

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    private final UUID gestor = UUID.randomUUID();
    private Document validadorV2;

    @BeforeEach
    void preparar() {
        SolicitudesAtractivo.filaDeCuenta(base(), gestor, "Gestor", 1);
        validadorV2 = base().listCollections().filter(Filters.eq("name", "atractivos")).first()
                .get("options", Document.class).get("validator", Document.class);
    }

    @AfterEach
    void restaurar() {
        validador(validadorV2);
        SolicitudesAtractivo.limpiar(base(), gestor);
    }

    @Test
    void siElInsertFallaLasImagenesYaGuardadasSeBorran() throws Exception {
        long archivos = contar("imagenes.files");
        long trozos = contar("imagenes.chunks");
        String nombre = "Atractivo que no se inserta " + UUID.randomUUID();
        validador(RECHAZA_TODO);

        MvcResult resultado = mvc.perform(registro(tokenGestor(gestor, 1), datos(nombre),
                parteImagen("a.jpg", imagen(FIRMA_JPEG, 300 * 1024)), parteImagen("b.png", imagen(FIRMA_PNG, 2048))))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(500);
        assertThat(JsonPath.<String>read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.codigo")).isEqualTo("ERROR_INTERNO");
        assertThat(contar("imagenes.files")).isEqualTo(archivos);
        assertThat(contar("imagenes.chunks")).isEqualTo(trozos);
        assertThat(base().getCollection("atractivos").countDocuments(Filters.eq("nombre", nombre))).isZero();
    }

    private void validador(Document validador) {
        base().runCommand(new Document("collMod", "atractivos").append("validator", validador)
                .append("validationLevel", "strict").append("validationAction", "error"));
    }

    private long contar(String coleccion) {
        return base().getCollection(coleccion).countDocuments();
    }

    private MongoDatabase base() {
        return mongo.getDb();
    }
}
