package co.edu.unimagdalena.samaria360.catalog.infrastructure.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-004: la documentación de MongoDB 7.0 no dice si $geoNear se permite como primera etapa del pipeline de
 * $unionWith (solo lo muestra dentro de $lookup). Esta prueba lo verifica contra mongo:7.0 real; la alternativa A
 * de la búsqueda combinada depende de ello.
 */
@Testcontainers
class GeoNearEnUnionWithFactibilidadTest {

    private static final List<Double> PARQUE_DE_LOS_NOVIOS = List.of(-74.2117, 11.2420);

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0")
            .withCopyFileToContainer(MountableFile.forHostPath(Path.of("../infra/mongo/init")), "/scripts");

    private static MongoClient cliente;

    @BeforeAll
    static void cargarScripts() throws Exception {
        for (String script : List.of("01-validadores.js", "02-indices.js", "03-semilla-catalogo.js")) {
            ExecResult resultado = MONGO.execInContainer("mongosh", "--quiet", "/scripts/" + script);
            assertThat(resultado.getExitCode()).as(resultado.getStderr()).isZero();
        }
        cliente = MongoClients.create(MONGO.getConnectionString());
    }

    @AfterAll
    static void cerrar() {
        if (cliente != null) {
            cliente.close();
        }
    }

    @Test
    void geoNearFuncionaComoPrimeraEtapaDelPipelineDeUnionWith() {
        Document cerca = new Document("type", "Point").append("coordinates", PARQUE_DE_LOS_NOVIOS);
        List<Document> pipeline = List.of(
                new Document("$geoNear", new Document("near", cerca)
                        .append("key", "coordenadas")
                        .append("distanceField", "distanciaMetros")
                        .append("spherical", true)
                        .append("query", new Document("estadoPublicacion", "Publicado").append("prestadorActivo", true))),
                new Document("$project", new Document("tipo", "servicio").append("nombre", "$nombreComercial")
                        .append("distanciaMetros", 1)),
                new Document("$unionWith", new Document("coll", "atractivos").append("pipeline", List.of(
                        new Document("$geoNear", new Document("near", cerca)
                                .append("key", "coordenadas")
                                .append("distanceField", "distanciaMetros")
                                .append("spherical", true)),
                        new Document("$project", new Document("tipo", "atractivo").append("nombre", 1)
                                .append("distanciaMetros", 1))))),
                new Document("$sort", new Document("distanciaMetros", 1).append("_id", 1)));

        List<Document> resultados = cliente.getDatabase("catalog_db").getCollection("servicios")
                .aggregate(pipeline).into(new ArrayList<>());

        Map<String, Long> porTipo = resultados.stream()
                .collect(Collectors.groupingBy(d -> d.getString("tipo"), Collectors.counting()));
        assertThat(porTipo).containsEntry("servicio", 11L).containsEntry("atractivo", 7L);
        assertThat(resultados).allSatisfy(d -> assertThat(d.get("distanciaMetros")).isInstanceOf(Double.class));

        Map<String, Double> distancias = resultados.stream()
                .collect(Collectors.toMap(d -> d.getString("nombre"), d -> d.getDouble("distanciaMetros")));
        // Ambos están a pocas cuadras del Parque de los Novios.
        assertThat(distancias.get("Hotel Casa Tayrona")).isLessThan(1000);
        assertThat(distancias.get("Catedral Basílica de Santa Marta")).isLessThan(1000);
        // Minca está a ~13 km: la distancia del atractivo se calcula de verdad, no es un valor por defecto.
        assertThat(distancias.get("Cascada El Salto")).isBetween(8_000.0, 20_000.0);
        // El $sort final intercala las dos colecciones por distancia.
        assertThat(resultados).extracting(d -> d.getDouble("distanciaMetros")).isSorted();
    }
}
