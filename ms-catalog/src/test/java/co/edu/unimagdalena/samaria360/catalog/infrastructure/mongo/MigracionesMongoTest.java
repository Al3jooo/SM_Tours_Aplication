package co.edu.unimagdalena.samaria360.catalog.infrastructure.mongo;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migraciones de catalog_db (infra/mongo/migraciones) con el mismo camino que Docker Compose:
 * <ul>
 *   <li><b>nuevo</b>: un volumen nuevo, línea base congelada (init/01, 02 y 03) y después las migraciones;</li>
 *   <li><b>existente</b>: un volumen creado con los scripts anteriores a congelar la línea base (01 con los
 *   validadores en línea), al que solo se le aplican las migraciones;</li>
 *   <li><b>errores</b>: un volumen v1 con datos que M001 no puede convertir, y las migraciones sin semilla.</li>
 * </ul>
 * Los dos primeros deben converger: mismos documentos (salvo los ObjectId aleatorios), validadores e índices.
 */
@Testcontainers
class MigracionesMongoTest {

    private static final Path ESQUEMAS = Path.of("../docs/sad/modelo-nosql");
    private static final Path VALIDADOR_M001 =
            MongoDePrueba.MIGRACIONES.resolve("esquema/M001__servicios.v2.schema.json");
    private static final List<String> MIGRACIONES = List.of("M001__unidades-alojamiento",
            "M002__semilla-suite-casa-tayrona");
    private static final int VALIDACION_DOCUMENTO_FALLIDA = 121;

    @Container
    static final MongoDBContainer NUEVO = MongoDePrueba.contenedor();
    @Container
    static final MongoDBContainer EXISTENTE = MongoDePrueba.contenedor();
    @Container
    static final MongoDBContainer ERRORES = MongoDePrueba.contenedor();

    private static MongoClient clienteNuevo;
    private static MongoClient clienteExistente;
    private static MongoClient clienteErrores;
    private static MongoDatabase nuevo;
    private static MongoDatabase existente;
    private static MongoDatabase errores;

    @BeforeAll
    static void preparar() throws Exception {
        MongoDePrueba.prepararCatalogoCompleto(NUEVO);

        volumenV1(EXISTENTE);
        MongoDePrueba.exigirExito("aplicar.js", MongoDePrueba.migrar(EXISTENTE, true));

        volumenV1(ERRORES);

        clienteNuevo = MongoClients.create(NUEVO.getConnectionString());
        clienteExistente = MongoClients.create(EXISTENTE.getConnectionString());
        clienteErrores = MongoClients.create(ERRORES.getConnectionString());
        nuevo = clienteNuevo.getDatabase("catalog_db");
        existente = clienteExistente.getDatabase("catalog_db");
        errores = clienteErrores.getDatabase("catalog_db");
    }

    /** Un volumen creado con los scripts anteriores: 01 con los validadores en línea, y 02 y 03 sin cambios. */
    private static void volumenV1(MongoDBContainer mongo) throws Exception {
        MongoDePrueba.exigirExito("01 en línea", MongoDePrueba.ejecutar(mongo, "/recursos/01-validadores-en-linea-v1.js"));
        MongoDePrueba.ejecutarScripts(mongo, List.of("02-indices.js", "03-semilla-catalogo.js"));
    }

    @AfterAll
    static void cerrar() {
        for (MongoClient cliente : new MongoClient[]{clienteNuevo, clienteExistente, clienteErrores}) {
            if (cliente != null) {
                cliente.close();
            }
        }
    }

    // ------------------------------------------------------------------ validador v2

    @ParameterizedTest
    @ValueSource(strings = {"servicios", "atractivos"})
    void despuesDeLasMigracionesElValidadorEsElDeDocs(String coleccion) throws IOException {
        Document esperado = Document.parse(Files.readString(ESQUEMAS.resolve(coleccion + ".schema.json")));

        assertThat(opciones(nuevo, coleccion).get("validator", Document.class)).isEqualTo(esperado);
        assertThat(opciones(nuevo, coleccion).getString("validationLevel")).isEqualTo("strict");
        assertThat(opciones(nuevo, coleccion).getString("validationAction")).isEqualTo("error");
    }

    /** Mientras no exista una migración posterior que cambie servicios, la copia de M001 es la última versión. */
    @Test
    void laCopiaDelValidadorDeM001EsLaUltimaVersion() throws IOException {
        assertThat(Document.parse(Files.readString(VALIDADOR_M001)))
                .isEqualTo(Document.parse(Files.readString(ESQUEMAS.resolve("servicios.schema.json"))));
    }

    @Test
    void elValidadorAceptaUnAlojamientoV2Valido() {
        MongoCollection<Document> servicios = nuevo.getCollection("servicios");
        Document alojamiento = alojamientoV2();

        servicios.insertOne(alojamiento);
        servicios.deleteOne(Filters.eq("_id", alojamiento.getObjectId("_id")));
    }

    @Test
    void elValidadorRechazaUnAlojamientoSinUnidades() {
        assertRechazado(alojamientoV2(d -> unidades(d).clear()));
        assertRechazado(alojamientoV2(d -> d.get("detalleCategoria", Document.class).remove("unidades")));
    }

    @Test
    void elValidadorRechazaUnIdDeUnidadRepetido() {
        assertRechazado(alojamientoV2(d -> unidades(d).get(1).put("idUnidad", unidades(d).get(0).get("idUnidad"))));
    }

    @Test
    void elValidadorRechazaNumerosYPreciosQueNoSonPositivos() {
        assertRechazado(alojamientoV2(d -> {
            unidades(d).get(0).put("precioNoche", Decimal128.parse("0"));
            d.put("precioBase", Decimal128.parse("0"));
        }));
        assertRechazado(alojamientoV2(d -> unidades(d).get(0).put("numHabitaciones", 0)));
        assertRechazado(alojamientoV2(d -> unidades(d).get(0).put("capacidadMaxHuespedes", 0)));
    }

    @Test
    void elValidadorRechazaUnPrecioBaseQueNoEsElMenorPrecioPorNoche() {
        assertRechazado(alojamientoV2(d -> d.put("precioBase", Decimal128.parse("260000"))));
        assertRechazado(alojamientoV2(d -> d.remove("precioBase")));
    }

    @Test
    void elValidadorRechazaLosCamposDelFormatoV1() {
        assertRechazado(alojamientoV2(d -> d.get("detalleCategoria", Document.class).append("tipoHabitacion", "Doble")));
        assertRechazado(alojamientoV2(d -> unidades(d).get(0).put("idUnidad", "no-es-un-objectid")));
    }

    // ------------------------------------------------------------------ conversión

    @Test
    void cadaAlojamientoV1QuedaConUnaUnidadConSusMismosDatos() {
        for (Document alojamiento : existente.getCollection("servicios").find(Filters.eq("categoria", "Alojamiento"))) {
            Document detalle = alojamiento.get("detalleCategoria", Document.class);
            Document primera = detalle.getList("unidades", Document.class).get(0);

            assertThat(detalle.keySet()).as("%s", alojamiento.get("nombreComercial"))
                    .containsExactlyInAnyOrder("serviciosIncluidos", "unidades");
            assertThat(primera.get("idUnidad")).isInstanceOf(ObjectId.class);
            assertThat(primera.getString("nombre")).isEqualTo(primera.getString("tipoHabitacion"));
            assertThat(primera.get("precioNoche")).isEqualTo(alojamiento.get("precioBase"));
        }
        assertThat(unidadesDe(existente, "Cabañas El Roble")).singleElement().satisfies(unidad -> {
            assertThat(unidad.getString("tipoHabitacion")).isEqualTo("Cabaña");
            assertThat(unidad.getInteger("numHabitaciones")).isEqualTo(6);
            assertThat(unidad.getInteger("capacidadMaxHuespedes")).isEqualTo(4);
        });
    }

    @Test
    void laSemillaDeM002AgregaLaSuiteSinCambiarElPrecioDesde() {
        List<Document> unidades = unidadesDe(existente, "Hotel Casa Tayrona");

        assertThat(unidades).extracting(unidad -> unidad.getString("nombre")).containsExactly("Doble", "Suite familiar");
        assertThat(unidades.get(1).get("precioNoche")).isEqualTo(Decimal128.parse("260000"));
        assertThat(existente.getCollection("servicios").find(Filters.eq("nombreComercial", "Hotel Casa Tayrona"))
                .first().get("precioBase")).isEqualTo(Decimal128.parse("150000"));
    }

    @Test
    void lasMigracionesQuedanRegistradas() {
        assertThat(idsMigraciones(nuevo)).containsExactlyElementsOf(MIGRACIONES);
        assertThat(idsMigraciones(existente)).containsExactlyElementsOf(MIGRACIONES);
    }

    @Test
    void volverAAplicarlasNoCambiaNadaNiLosIdsDeUnidad() throws Exception {
        List<Document> antes = documentos(existente, "servicios");

        ExecResult segunda = MongoDePrueba.migrar(EXISTENTE, true);

        MongoDePrueba.exigirExito("aplicar.js", segunda);
        assertThat(segunda.getStdout()).contains("ya aplicada: M001", "ya aplicada: M002");
        assertThat(documentos(existente, "servicios")).isEqualTo(antes);
        assertThat(idsMigraciones(existente)).containsExactlyElementsOf(MIGRACIONES);
    }

    // ------------------------------------------------------------------ convergencia

    @ParameterizedTest
    @ValueSource(strings = {"servicios", "atractivos"})
    void unVolumenNuevoYUnoExistenteTerminanIguales(String coleccion) {
        assertThat(sinIdsAleatorios(documentos(existente, coleccion)))
                .isNotEmpty()
                .isEqualTo(sinIdsAleatorios(documentos(nuevo, coleccion)));
        assertThat(opciones(existente, coleccion)).isEqualTo(opciones(nuevo, coleccion));
        assertThat(indices(existente, coleccion)).isEqualTo(indices(nuevo, coleccion));
    }

    // ------------------------------------------------------------------ errores y semilla

    /** En orden: primero el caso que aborta y, ya corregido el dato, la aplicación sin semilla. */
    @Test
    void unAlojamientoSinPrecioAbortaSinTocarNadaYSinSemillaNoSeAplicaM002() throws Exception {
        MongoCollection<Document> servicios = errores.getCollection("servicios");
        Document validadorV1 = opciones(errores, "servicios");
        Document coordenadas = errores.getCollection("servicios")
                .find(Filters.eq("nombreComercial", "Cabañas El Roble")).first().get("coordenadas", Document.class);
        Document v1SinPrecio = new Document("prestadorId", "00000000-0000-4000-8000-000000000002")
                .append("nombreComercial", "Alojamiento sin precio")
                .append("categoria", "Alojamiento")
                .append("coordenadas", coordenadas)
                .append("estadoPublicacion", "Pendiente")
                .append("prestadorActivo", true)
                .append("detalleCategoria", new Document("tipoHabitacion", "Doble")
                        .append("numHabitaciones", 2)
                        .append("capacidadMaxHuespedes", 2));
        servicios.insertOne(v1SinPrecio);
        List<Document> antes = documentos(errores, "servicios");

        ExecResult abortada = MongoDePrueba.migrar(ERRORES, false);

        assertThat(abortada.getExitCode()).isNotZero();
        assertThat(abortada.getStdout() + abortada.getStderr())
                .contains("sin precioBase", v1SinPrecio.getObjectId("_id").toHexString());
        assertThat(documentos(errores, "servicios")).isEqualTo(antes);
        assertThat(opciones(errores, "servicios")).isEqualTo(validadorV1);
        assertThat(idsMigraciones(errores)).isEmpty();

        servicios.deleteOne(Filters.eq("_id", v1SinPrecio.getObjectId("_id")));
        MongoDePrueba.exigirExito("aplicar.js", MongoDePrueba.migrar(ERRORES, false));

        assertThat(idsMigraciones(errores)).containsExactly("M001__unidades-alojamiento");
        assertThat(unidadesDe(errores, "Hotel Casa Tayrona")).hasSize(1);
    }

    // ------------------------------------------------------------------ auxiliares

    private static Document opciones(MongoDatabase base, String coleccion) {
        return base.listCollections().filter(Filters.eq("name", coleccion)).first().get("options", Document.class);
    }

    private static List<String> idsMigraciones(MongoDatabase base) {
        List<String> ids = new ArrayList<>();
        base.getCollection("migraciones").find().sort(Sorts.ascending("_id"))
                .forEach(migracion -> ids.add(migracion.getString("_id")));
        return ids;
    }

    private static List<Document> documentos(MongoDatabase base, String coleccion) {
        String nombre = "servicios".equals(coleccion) ? "nombreComercial" : "nombre";
        return base.getCollection(coleccion).find().sort(Sorts.ascending(nombre)).into(new ArrayList<>());
    }

    /** Sin _id ni idUnidad: son ObjectId aleatorios en cada volumen. */
    private static List<Document> sinIdsAleatorios(List<Document> documentos) {
        List<Document> limpios = new ArrayList<>();
        for (Document original : documentos) {
            Document documento = Document.parse(original.toJson());
            documento.remove("_id");
            Document detalle = documento.get("detalleCategoria", Document.class);
            if (detalle != null && detalle.containsKey("unidades")) {
                detalle.getList("unidades", Document.class).forEach(unidad -> unidad.remove("idUnidad"));
            }
            limpios.add(documento);
        }
        return limpios;
    }

    private static List<Document> indices(MongoDatabase base, String coleccion) {
        List<Document> indices = new ArrayList<>();
        base.getCollection(coleccion).listIndexes().forEach(indice -> indices.add(
                new Document("name", indice.get("name")).append("key", indice.get("key"))));
        indices.sort((a, b) -> a.getString("name").compareTo(b.getString("name")));
        return indices;
    }

    private static List<Document> unidadesDe(MongoDatabase base, String nombreComercial) {
        return base.getCollection("servicios").find(Filters.eq("nombreComercial", nombreComercial)).first()
                .get("detalleCategoria", Document.class).getList("unidades", Document.class);
    }

    @SuppressWarnings("unchecked")
    private static List<Document> unidades(Document alojamiento) {
        return (List<Document>) alojamiento.get("detalleCategoria", Document.class).get("unidades");
    }

    private static Document alojamientoV2() {
        return alojamientoV2(documento -> {
        });
    }

    /** Un alojamiento v2 válido (Doble 150.000 y Suite 260.000) con un cambio aplicado. */
    private static Document alojamientoV2(Consumer<Document> cambio) {
        List<Document> unidades = new ArrayList<>(List.of(
                new Document("idUnidad", new ObjectId()).append("nombre", "Doble").append("tipoHabitacion", "Doble")
                        .append("numHabitaciones", 12).append("capacidadMaxHuespedes", 2)
                        .append("precioNoche", Decimal128.parse("150000")),
                new Document("idUnidad", new ObjectId()).append("nombre", "Suite familiar")
                        .append("tipoHabitacion", "Suite").append("numHabitaciones", 3)
                        .append("capacidadMaxHuespedes", 4).append("precioNoche", Decimal128.parse("260000"))));
        Document documento = new Document("_id", new ObjectId())
                .append("prestadorId", "00000000-0000-4000-8000-000000000002")
                .append("nombreComercial", "Hotel de prueba")
                .append("categoria", "Alojamiento")
                .append("coordenadas", new Document("type", "Point").append("coordinates", List.of(-74.2125, 11.2425)))
                .append("estadoPublicacion", "Pendiente")
                .append("prestadorActivo", true)
                .append("precioBase", Decimal128.parse("150000"))
                .append("detalleCategoria", new Document("serviciosIncluidos", List.of("WiFi"))
                        .append("unidades", unidades));
        cambio.accept(documento);
        return documento;
    }

    private static void assertRechazado(Document documento) {
        assertThatThrownBy(() -> nuevo.getCollection("servicios").insertOne(documento))
                .isInstanceOf(MongoWriteException.class)
                .satisfies(e -> assertThat(((MongoWriteException) e).getError().getCode())
                        .isEqualTo(VALIDACION_DOCUMENTO_FALLIDA));
    }
}
