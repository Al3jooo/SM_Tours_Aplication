package co.edu.unimagdalena.samaria360.catalog.infrastructure.mongo;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LÍNEA BASE CONGELADA (formato v1): ejecuta infra/mongo/init/01, 02 y 03 con mongosh dentro de mongo:7.0 (la imagen
 * de docker-compose), igual que el entrypoint de Docker, y verifica el resultado con el driver de Java. Sin
 * migraciones: el formato actual (v2) lo verifica {@link MigracionesMongoTest}.
 */
@Testcontainers
class ScriptsInicializacionMongoTest {

    private static final Path SCRIPTS = Path.of("../infra/mongo/init");
    private static final String PRESTADOR = "00000000-0000-4000-8000-000000000002";
    private static final String PRESTADOR_INACTIVO = "00000000-0000-4000-8000-000000000005";
    private static final String GESTOR = "00000000-0000-4000-8000-000000000003";
    private static final int VALIDACION_DOCUMENTO_FALLIDA = 121;

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0")
            .withCopyFileToContainer(MountableFile.forHostPath(SCRIPTS), "/scripts");

    private static MongoClient cliente;
    private static MongoDatabase catalogo;

    @BeforeAll
    static void ejecutarScripts() throws Exception {
        for (String script : List.of("01-validadores.js", "02-indices.js", "03-semilla-catalogo.js")) {
            ExecResult resultado = MONGO.execInContainer("mongosh", "--quiet", "/scripts/" + script);
            assertThat(resultado.getExitCode())
                    .as("%s falló:%n%s%s", script, resultado.getStdout(), resultado.getStderr())
                    .isZero();
        }
        cliente = MongoClients.create(MONGO.getConnectionString());
        catalogo = cliente.getDatabase("catalog_db");
    }

    @AfterAll
    static void cerrar() {
        if (cliente != null) {
            cliente.close();
        }
    }

    // ------------------------------------------------------------------ validadores

    @ParameterizedTest
    @ValueSource(strings = {"servicios", "atractivos"})
    void elValidadorEsElV1CongeladoJuntoALosScripts(String coleccion) throws IOException {
        Document opciones = catalogo.listCollections().filter(Filters.eq("name", coleccion)).first()
                .get("options", Document.class);
        Document esperado = Document.parse(Files.readString(SCRIPTS.resolve(coleccion + ".v1.schema.json")));

        assertThat(opciones.get("validator", Document.class)).isEqualTo(esperado);
        assertThat(opciones.getString("validationLevel")).isEqualTo("strict");
        assertThat(opciones.getString("validationAction")).isEqualTo("error");
    }

    @Test
    void elValidadorRechazaUnTourSinAforoMaxGrupo() {
        Document tour = tourValido();
        tour.get("detalleCategoria", Document.class).remove("aforoMaxGrupo");

        assertRechazado(tour);
    }

    @Test
    void elValidadorRechazaUnAlojamientoConCamposDeGastronomia() {
        Document alojamiento = tourValido()
                .append("categoria", "Alojamiento")
                .append("detalleCategoria", new Document("tipoCocina", "Mediterránea")
                        .append("aforoMaxTurno", 60)
                        .append("rangoPrecioPlato", "$50.000 - $120.000"));

        assertRechazado(alojamiento);
    }

    @Test
    void elValidadorRechazaUnServicioIncluidoFueraDelVocabulario() {
        assertRechazado(alojamiento(List.of("WiFi", "Jacuzzi")));
        assertRechazado(alojamiento(List.of("wifi"))); // el nombre canónico distingue mayúsculas
    }

    @Test
    void elValidadorAceptaLosCuatroServiciosIncluidos() {
        MongoCollection<Document> servicios = catalogo.getCollection("servicios");
        Document alojamiento = alojamiento(List.of("WiFi", "Desayuno", "Piscina", "Parqueadero"));

        servicios.insertOne(alojamiento);
        servicios.deleteOne(Filters.eq("_id", alojamiento.getObjectId("_id")));
    }

    @Test
    void laSemillaSoloUsaElVocabularioDeServiciosIncluidos() {
        assertThat(catalogo.getCollection("servicios")
                .distinct("detalleCategoria.serviciosIncluidos", String.class).into(new HashSet<>()))
                .containsExactlyInAnyOrder("WiFi", "Desayuno", "Piscina", "Parqueadero");
    }

    @Test
    void elValidadorAceptaUnTourValido() {
        MongoCollection<Document> servicios = catalogo.getCollection("servicios");
        Document tour = tourValido();

        servicios.insertOne(tour);
        servicios.deleteOne(Filters.eq("_id", tour.getObjectId("_id"))); // no altera los conteos de la semilla
    }

    // ------------------------------------------------------------------ semilla

    @Test
    void laSemillaDeServiciosEstaCompleta() {
        MongoCollection<Document> servicios = catalogo.getCollection("servicios");

        assertThat(servicios.countDocuments()).isEqualTo(14);
        assertThat(servicios.countDocuments(Filters.eq("categoria", "Alojamiento"))).isEqualTo(4);
        assertThat(servicios.countDocuments(Filters.eq("categoria", "Gastronomia"))).isEqualTo(4);
        assertThat(servicios.countDocuments(Filters.eq("categoria", "Tour"))).isEqualTo(4);
        assertThat(servicios.countDocuments(Filters.eq("categoria", "AlquilerEquipo"))).isEqualTo(2);
        // RF37: todos tienen precio, aunque el esquema lo permita opcional.
        assertThat(servicios.countDocuments(Filters.type("precioBase", "decimal"))).isEqualTo(14);
    }

    @Test
    void laSemillaPermiteProbarLosFiltrosDeLaBusquedaPublica() {
        MongoCollection<Document> servicios = catalogo.getCollection("servicios");

        assertThat(servicios.countDocuments(Filters.eq("estadoPublicacion", "Pendiente"))).isEqualTo(2);
        assertThat(servicios.countDocuments(Filters.eq("prestadorActivo", false))).isEqualTo(1);
        assertThat(servicios.countDocuments(
                Filters.and(Filters.eq("estadoPublicacion", "Publicado"), Filters.eq("prestadorActivo", true))))
                .isEqualTo(11);

        Document inactivo = servicios.find(Filters.eq("prestadorActivo", false)).first();
        assertThat(inactivo.getString("nombreComercial")).isEqualTo("Snorkel en Bahía Concha");
        assertThat(inactivo.getString("prestadorId")).isEqualTo(PRESTADOR_INACTIVO);
    }

    @Test
    void laSemillaDeAtractivosEstaCompleta() {
        MongoCollection<Document> atractivos = catalogo.getCollection("atractivos");

        assertThat(atractivos.countDocuments()).isEqualTo(7);
        assertThat(atractivos.distinct("gestorId", String.class).into(new ArrayList<>())).containsExactly(GESTOR);
        assertThat(atractivos.countDocuments(Filters.ne("estadoOperativo", "Abierto"))).isEqualTo(2);
    }

    @Test
    void losServiciosReferencianALosPrestadoresDeLaSemillaDeMsAuth() {
        assertThat(catalogo.getCollection("servicios").distinct("prestadorId", String.class).into(new HashSet<>()))
                .containsExactlyInAnyOrder(PRESTADOR, PRESTADOR_INACTIVO);
    }

    @Test
    void usaLosNombresDeLosWireframes() {
        Set<String> servicios = catalogo.getCollection("servicios").distinct("nombreComercial", String.class)
                .into(new HashSet<>());
        Set<String> atractivos = catalogo.getCollection("atractivos").distinct("nombre", String.class)
                .into(new HashSet<>());

        assertThat(servicios).contains("Hotel Casa Tayrona", "Restaurante Ouzo Santa Marta", "Tour de Café en Minca",
                "Cabañas El Roble", "Mercado Gastronómico La Plaza", "Tour de café Samaria Alta");
        assertThat(atractivos).contains("Cascada Quebrada Valencia", "Cascada El Salto");
    }

    @ParameterizedTest
    @ValueSource(strings = {"servicios", "atractivos"})
    void lasCoordenadasSonDoublesEnOrdenLongitudLatitudDentroDeSantaMarta(String coleccion) {
        for (Document documento : catalogo.getCollection(coleccion).find()) {
            List<?> coordenadas = documento.get("coordenadas", Document.class).get("coordinates", List.class);
            assertThat(coordenadas).hasSize(2).allMatch(Double.class::isInstance);
            double longitud = (Double) coordenadas.get(0);
            double latitud = (Double) coordenadas.get(1);
            // Un orden invertido [latitud, longitud] caería fuera de estos rangos.
            assertThat(longitud).as("longitud de %s", documento.get("_id")).isBetween(-74.3, -73.7);
            assertThat(latitud).as("latitud de %s", documento.get("_id")).isBetween(11.0, 11.4);
        }
    }

    // ------------------------------------------------------------------ índices

    @ParameterizedTest
    @ValueSource(strings = {"servicios", "atractivos"})
    void existeElIndice2dsphere(String coleccion) {
        assertThat(indice(coleccion, "coordenadas_2dsphere").get("key", Document.class))
                .isEqualTo(new Document("coordenadas", "2dsphere"));
    }

    @Test
    void existeElIndiceCompuestoDeBusquedaPublica() {
        Document clave = indice("servicios", "busqueda_publica").get("key", Document.class);

        assertThat(new ArrayList<>(clave.keySet()))
                .containsExactly("estadoPublicacion", "prestadorActivo", "categoria");
    }

    // ------------------------------------------------------------------ auxiliares

    private static Document indice(String coleccion, String nombre) {
        for (Document indice : catalogo.getCollection(coleccion).listIndexes()) {
            if (nombre.equals(indice.getString("name"))) {
                return indice;
            }
        }
        throw new AssertionError("No existe el índice " + nombre + " en " + coleccion);
    }

    private static void assertRechazado(Document documento) {
        assertThatThrownBy(() -> catalogo.getCollection("servicios").insertOne(documento))
                .isInstanceOf(MongoWriteException.class)
                .satisfies(e -> assertThat(((MongoWriteException) e).getError().getCode())
                        .isEqualTo(VALIDACION_DOCUMENTO_FALLIDA));
    }

    private static Document alojamiento(List<String> serviciosIncluidos) {
        return tourValido()
                .append("categoria", "Alojamiento")
                .append("detalleCategoria", new Document("tipoHabitacion", "Doble")
                        .append("numHabitaciones", 4)
                        .append("capacidadMaxHuespedes", 2)
                        .append("serviciosIncluidos", serviciosIncluidos));
    }

    private static Document tourValido() {
        return new Document("prestadorId", PRESTADOR)
                .append("nombreComercial", "Tour de prueba")
                .append("categoria", "Tour")
                .append("coordenadas", new Document("type", "Point").append("coordinates", List.of(-74.119, 11.1435)))
                .append("estadoPublicacion", "Pendiente")
                .append("prestadorActivo", true)
                .append("precioBase", Decimal128.parse("80000"))
                .append("imagenes", List.of())
                .append("detalleCategoria", new Document("puntoEncuentro", "Iglesia de Minca")
                        .append("duracionHoras", Decimal128.parse("4"))
                        .append("aforoMaxGrupo", 12));
    }
}
