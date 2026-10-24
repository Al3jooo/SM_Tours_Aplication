package co.edu.unimagdalena.samaria360.catalog.infrastructure.mongo;

import co.edu.unimagdalena.samaria360.catalog.domain.model.PreguntaFrecuente;
import co.edu.unimagdalena.samaria360.catalog.domain.model.TextoAcotado;
import com.mongodb.ErrorCategory;
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
import java.util.UUID;
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
    private static final Path VALIDADOR_M003 =
            MongoDePrueba.MIGRACIONES.resolve("esquema/M003__cuentas.v1.schema.json");
    private static final Path VALIDADOR_M005_FAQ = MongoDePrueba.MIGRACIONES.resolve("esquema/M005__faq.v1.schema.json");
    private static final Path VALIDADOR_M005_AVISOS =
            MongoDePrueba.MIGRACIONES.resolve("esquema/M005__avisos.v1.schema.json");
    private static final Path VALIDADOR_M007_FAVORITOS =
            MongoDePrueba.MIGRACIONES.resolve("esquema/M007__favoritos.v1.schema.json");
    private static final Path VALIDADOR_M007_HISTORIAL =
            MongoDePrueba.MIGRACIONES.resolve("esquema/M007__historial_busqueda.v1.schema.json");
    private static final List<String> MIGRACIONES = List.of("M001__unidades-alojamiento",
            "M002__semilla-suite-casa-tayrona", "M003__cuentas", "M004__indice-servicios-prestador",
            "M005__faq-avisos", "M006__semilla-faq-avisos", "M007__favoritos-historial");
    /** Fuera del plano básico: un punto de código, dos unidades UTF-16. */
    private static final String EMOJI = "🌴";
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
    @ValueSource(strings = {"servicios", "atractivos", "cuentas", "faq", "avisos", "favoritos", "historial_busqueda"})
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

    /** Mientras no exista una migración posterior que cambie cuentas, la copia de M003 es la última versión. */
    @Test
    void laCopiaDelValidadorDeM003EsLaUltimaVersion() throws IOException {
        assertThat(Document.parse(Files.readString(VALIDADOR_M003)))
                .isEqualTo(Document.parse(Files.readString(ESQUEMAS.resolve("cuentas.schema.json"))));
    }

    /** Mientras no exista una migración posterior que cambie faq o avisos, las copias de M005 son la última versión. */
    @Test
    void lasCopiasDeLosValidadoresDeM005SonLaUltimaVersion() throws IOException {
        assertThat(Document.parse(Files.readString(VALIDADOR_M005_FAQ)))
                .isEqualTo(Document.parse(Files.readString(ESQUEMAS.resolve("faq.schema.json"))));
        assertThat(Document.parse(Files.readString(VALIDADOR_M005_AVISOS)))
                .isEqualTo(Document.parse(Files.readString(ESQUEMAS.resolve("avisos.schema.json"))));
    }

    /** Mientras no exista una migración posterior que cambie favoritos o historial_busqueda, las de M007 son la última. */
    @Test
    void lasCopiasDeLosValidadoresDeM007SonLaUltimaVersion() throws IOException {
        assertThat(Document.parse(Files.readString(VALIDADOR_M007_FAVORITOS)))
                .isEqualTo(Document.parse(Files.readString(ESQUEMAS.resolve("favoritos.schema.json"))));
        assertThat(Document.parse(Files.readString(VALIDADOR_M007_HISTORIAL)))
                .isEqualTo(Document.parse(Files.readString(ESQUEMAS.resolve("historial_busqueda.schema.json"))));
    }

    // ------------------------------------------------------------------ favoritos e historial_busqueda (M007)

    @Test
    void m007CreaLosIndicesDeFavoritosEHistorialEnVolumenNuevoYExistente() {
        for (MongoDatabase base : List.of(nuevo, existente)) {
            assertThat(indicesConUnicidad(base, "favoritos")).contains(
                    new Document("name", "favoritos_turista_entidad").append("key", new Document("turistaId", 1)
                            .append("tipoEntidad", 1).append("entidadId", 1)).append("unique", true),
                    new Document("name", "favoritos_turista_fecha").append("key", new Document("turistaId", 1)
                            .append("fechaCreacion", -1).append("_id", -1)).append("unique", false));
            assertThat(indicesConUnicidad(base, "historial_busqueda")).contains(
                    new Document("name", "historial_turista_fecha").append("key", new Document("turistaId", 1)
                            .append("fecha", -1).append("_id", -1)).append("unique", false),
                    new Document("name", "historial_turista_clave").append("key", new Document("turistaId", 1)
                            .append("clave", 1)).append("unique", true));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"favoritos", "historial_busqueda"})
    void m007DejaLasMismasOpcionesEIndicesEnUnVolumenNuevoYEnUnoExistente(String coleccion) {
        assertThat(opciones(existente, coleccion)).isEqualTo(opciones(nuevo, coleccion));
        assertThat(indicesConUnicidad(existente, coleccion)).isEqualTo(indicesConUnicidad(nuevo, coleccion));
    }

    @Test
    void laColeccionFavoritosAceptaUnServicioYUnAtractivoYRechazaUnDuplicado() {
        MongoCollection<Document> favoritos = nuevo.getCollection("favoritos");
        String turista = UUID.randomUUID().toString();
        String entidad = new ObjectId().toHexString();
        favoritos.insertOne(favorito(turista, entidad, "servicio"));
        favoritos.insertOne(favorito(turista, entidad, "atractivo"));

        assertThatThrownBy(() -> favoritos.insertOne(favorito(turista, entidad, "servicio")))
                .isInstanceOf(MongoWriteException.class)
                .satisfies(e -> assertThat(((MongoWriteException) e).getError().getCategory())
                        .isEqualTo(ErrorCategory.DUPLICATE_KEY));
        favoritos.deleteMany(Filters.eq("turistaId", turista));
    }

    @Test
    void laColeccionFavoritosRechazaLoQueNoSeaUnFavoritoValido() {
        MongoCollection<Document> favoritos = nuevo.getCollection("favoritos");
        String turista = UUID.randomUUID().toString();
        String entidad = new ObjectId().toHexString();

        assertRechazado(favoritos, favorito(turista, entidad, "Servicio"));
        assertRechazado(favoritos, favorito(turista, entidad, "evento"));
        assertRechazado(favoritos, favorito(turista.toUpperCase(), entidad, "servicio"));
        assertRechazado(favoritos, favorito(turista, "no-es-un-objectid", "servicio"));
        assertRechazado(favoritos, favorito(turista, entidad, "servicio").append("nombre", "Desnormalizado"));
        Document sinFecha = favorito(turista, entidad, "servicio");
        sinFecha.remove("fechaCreacion");
        assertRechazado(favoritos, sinFecha);
    }

    @Test
    void laColeccionHistorialAceptaUnaBusquedaValidaYRechazaLaMismaClaveDelMismoTurista() {
        MongoCollection<Document> historial = nuevo.getCollection("historial_busqueda");
        String turista = UUID.randomUUID().toString();
        historial.insertOne(busqueda(turista, "{\"texto\":\"Playa\"}", "texto=playa"));
        historial.insertOne(busqueda(UUID.randomUUID().toString(), "{\"texto\":\"Playa\"}", "texto=playa"));

        assertThatThrownBy(() -> historial.insertOne(busqueda(turista, "{\"texto\":\"playa\"}", "texto=playa")))
                .isInstanceOf(MongoWriteException.class)
                .satisfies(e -> assertThat(((MongoWriteException) e).getError().getCategory())
                        .isEqualTo(ErrorCategory.DUPLICATE_KEY));
        historial.deleteMany(Filters.eq("clave", "texto=playa"));
    }

    @Test
    void laColeccionHistorialRechazaLoQueNoSeaUnaBusquedaValida() {
        MongoCollection<Document> historial = nuevo.getCollection("historial_busqueda");
        String turista = UUID.randomUUID().toString();

        assertRechazado(historial, busqueda(turista, "{\"texto\":\"Playa\"}", ""));
        assertRechazado(historial, busqueda(turista, "x".repeat(2001), "clave"));
        assertRechazado(historial, busqueda(turista, "{}", "c".repeat(2001)));
        assertRechazado(historial, busqueda(turista, "{}", "clave").append("lat", 11.2));
        Document conObjeto = busqueda(turista, "{}", "clave");
        conObjeto.put("criteriosBusqueda", new Document("texto", "Playa"));
        assertRechazado(historial, conObjeto);
        Document sinFecha = busqueda(turista, "{}", "clave");
        sinFecha.remove("fecha");
        assertRechazado(historial, sinFecha);
    }

    // ------------------------------------------------------------------ faq y avisos (M005, M006)

    @Test
    void laColeccionFaqAceptaPreguntasValidasHastaElLimiteEnPuntosDeCodigo() {
        MongoCollection<Document> faq = nuevo.getCollection("faq");

        for (Document pregunta : List.of(pregunta("¿Hay parqueadero?", "Sí."),
                pregunta("a".repeat(150), "b".repeat(2000)),
                pregunta(EMOJI.repeat(150), EMOJI.repeat(2000)),
                pregunta("Dos\nlíneas", "Primera línea.\n\nSegunda línea."))) {
            faq.insertOne(pregunta);
            faq.deleteOne(Filters.eq("_id", pregunta.getObjectId("_id")));
        }
    }

    @Test
    void laColeccionFaqRechazaLoQueNoSeaUnaPreguntaValida() {
        MongoCollection<Document> faq = nuevo.getCollection("faq");

        assertRechazado(faq, pregunta("", "Cuerpo"));
        assertRechazado(faq, pregunta("   ", "Cuerpo"));
        assertRechazado(faq, pregunta(" Título", "Cuerpo"));
        assertRechazado(faq, pregunta("Título\n", "Cuerpo"));
        assertRechazado(faq, pregunta("a".repeat(151), "Cuerpo"));
        assertRechazado(faq, pregunta(EMOJI.repeat(151), "Cuerpo"));
        assertRechazado(faq, pregunta("Título", "b".repeat(2001)));
        assertRechazado(faq, pregunta("Título", "Cuerpo").append("otro", "campo"));
        assertRechazado(faq, pregunta("Título", "Cuerpo").append("fechaActualizacion", "hoy"));
        Document sinFecha = pregunta("Título", "Cuerpo");
        sinFecha.remove("fechaActualizacion");
        assertRechazado(faq, sinFecha);
    }

    /**
     * El dominio recorta con {@link String#strip()}; el validador exige {@code \S} en los extremos. Todo lo que MongoDB
     * rechaza, el dominio también lo rechaza, y el texto que el dominio guarda (ya limpio) MongoDB lo acepta: también con
     * espacios Unicode que {@code strip()} no recorta (U+00A0, U+2007, U+202F) o sí recorta (U+2003, U+3000). El
     * dominio puede ser más estricto: recorta U+2003, que el {@code \S} de MongoDB acepta.
     */
    @ParameterizedTest
    @ValueSource(strings = {" ", " ", " ", " ", "　", "\u0085", "﻿", "\t", "\n"})
    void loQueRechazaElValidadorTambienLoRechazaElDominioConEspaciosEnLosExtremos(String espacio) {
        for (String texto : List.of("Hola" + espacio, espacio + "Hola")) {
            String codigo = String.format("U+%04X en «%s»", (int) espacio.charAt(0), texto);
            boolean dominioLoDaPorValido = texto.equals(TextoAcotado.limpiar(texto))
                    && TextoAcotado.validar(texto, PreguntaFrecuente.TITULO_MAXIMO).isEmpty();

            if (!aceptaComoTitulo(texto)) {
                assertThat(dominioLoDaPorValido).as(codigo).isFalse();
            }
            assertThat(aceptaComoTitulo(TextoAcotado.limpiar(texto))).as("limpio: " + codigo).isTrue();
        }
    }

    /** Lo que de verdad hace el {@code pattern} en un extremo: rechaza los espacios ASCII y acepta los Unicode. */
    @ParameterizedTest
    @ValueSource(strings = {" ", "\t", "\n", "\r", "\u000B", "\f"})
    void elValidadorRechazaUnEspacioAsciiEnCualquierExtremo(String espacio) {
        assertThat(aceptaComoTitulo("Hola" + espacio)).isFalse();
        assertThat(aceptaComoTitulo(espacio + "Hola")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", " ", " ", " ", "　", "\u0085", "﻿"})
    void elValidadorAceptaUnEspacioUnicodeEnUnExtremo(String espacio) {
        assertThat(aceptaComoTitulo("Hola" + espacio)).isTrue();
        assertThat(aceptaComoTitulo(espacio + "Hola")).isTrue();
    }

    /** U+00A0: ni {@code strip()} ni el {@code \S} de MongoDB lo tratan como espacio, así que se conserva y se acepta. */
    @Test
    void unEspacioDeNoSeparacionEnUnExtremoSeConservaYSeAcepta() {
        assertThat(TextoAcotado.limpiar("Hola ")).isEqualTo("Hola ");
        assertThat(aceptaComoTitulo("Hola ")).isTrue();
        assertThat(aceptaComoTitulo(" Hola")).isTrue();
    }

    @Test
    void laColeccionAvisosAceptaUnAvisoValidoDeCadaTipo() {
        MongoCollection<Document> avisos = nuevo.getCollection("avisos");

        for (String tipo : List.of("Clima", "Cierre", "Comunicado")) {
            for (Document aviso : List.of(aviso("Mensaje", tipo, true), aviso("m".repeat(300), tipo, false),
                    aviso(EMOJI.repeat(300), tipo, true))) {
                avisos.insertOne(aviso);
                avisos.deleteOne(Filters.eq("_id", aviso.getObjectId("_id")));
            }
        }
    }

    @Test
    void laColeccionAvisosRechazaLoQueNoSeaUnAvisoValido() {
        MongoCollection<Document> avisos = nuevo.getCollection("avisos");

        assertRechazado(avisos, aviso("", "Clima", true));
        assertRechazado(avisos, aviso(" Mensaje", "Clima", true));
        assertRechazado(avisos, aviso("m".repeat(301), "Clima", true));
        assertRechazado(avisos, aviso(EMOJI.repeat(301), "Clima", true));
        assertRechazado(avisos, aviso("Mensaje", "Lluvia", true));
        assertRechazado(avisos, aviso("Mensaje", "Clima", true).append("activo", "si"));
        assertRechazado(avisos, aviso("Mensaje", "Clima", true).append("vigenteHasta", new java.util.Date()));
        Document sinFecha = aviso("Mensaje", "Clima", true);
        sinFecha.remove("fechaPublicacion");
        assertRechazado(avisos, sinFecha);
    }

    @Test
    void m005CreaLosIndicesDeFaqYAvisosEnVolumenNuevoYExistente() {
        for (MongoDatabase base : List.of(nuevo, existente)) {
            assertThat(base.getCollection("faq").listIndexes().into(new ArrayList<>())).anySatisfy(indice -> {
                assertThat(indice.getString("name")).isEqualTo("faq_titulo");
                assertThat(indice.get("key", Document.class)).isEqualTo(new Document("titulo", 1).append("_id", 1));
                assertThat(indice.get("collation", Document.class).getString("locale")).isEqualTo("es");
                assertThat(indice.get("collation", Document.class).getInteger("strength")).isEqualTo(1);
            });
            assertThat(indices(base, "avisos")).contains(
                    new Document("name", "avisos_activos").append("key",
                            new Document("activo", 1).append("fechaPublicacion", -1).append("_id", -1)),
                    new Document("name", "avisos_fecha").append("key",
                            new Document("fechaPublicacion", -1).append("_id", -1)));
        }
    }

    @Test
    void laSemillaDeM006TraeCincoPreguntasYUnAvisoActivo() {
        assertThat(existente.getCollection("faq").countDocuments()).isEqualTo(5);
        assertThat(existente.getCollection("avisos").find().into(new ArrayList<>())).singleElement()
                .satisfies(aviso -> {
                    assertThat(aviso.getString("tipo")).isEqualTo("Cierre");
                    assertThat(aviso.getBoolean("activo")).isTrue();
                });
    }

    // ------------------------------------------------------------------ cuentas (M003) y su índice (M004)

    @ParameterizedTest
    @ValueSource(strings = {"Pendiente", "Activo", "Rechazado", "Inactivo", "Desactivado"})
    void laColeccionCuentasAceptaCadaEstadoDerivado(String estado) {
        MongoCollection<Document> cuentas = nuevo.getCollection("cuentas");
        Document cuenta = cuenta("00000000-0000-4000-8000-0000000000e1", "Prestador", estado, 1L);

        cuentas.insertOne(cuenta);
        cuentas.deleteOne(Filters.eq("_id", cuenta.getString("_id")));
    }

    @Test
    void laColeccionCuentasRechazaLoQueNoSeaUnaProyeccionValida() {
        MongoCollection<Document> cuentas = nuevo.getCollection("cuentas");
        String id = "00000000-0000-4000-8000-0000000000e2";

        assertRechazado(cuentas, cuenta(id, "Prestador", "Suspendido", 1L));
        assertRechazado(cuentas, cuenta(id, "Visitante", "Activo", 1L));
        assertRechazado(cuentas, cuenta(id, "Prestador", "Activo", 0L));
        assertRechazado(cuentas, cuenta(id, "Prestador", "Activo", 1)); // int32: la versión es long
        assertRechazado(cuentas, cuenta("no-es-un-uuid", "Prestador", "Activo", 1L));
        assertRechazado(cuentas, cuenta(id, "Prestador", "Activo", 1L).append("otro", "campo"));
        assertRechazado(cuentas, cuenta(id, "Prestador", "Activo", 1L).append("actualizadoEn", "ayer"));
    }

    @Test
    void elUpsertCondicionalIgnoraUnaVersionIgualOMenor() {
        MongoCollection<Document> cuentas = nuevo.getCollection("cuentas");
        String id = "00000000-0000-4000-8000-0000000000e3";
        cuentas.insertOne(cuenta(id, "Turista", "Desactivado", 5L));

        assertThatThrownBy(() -> cuentas.updateOne(
                Filters.and(Filters.eq("_id", id), Filters.lt("version", 5L)),
                // Como CuentaMongoAdapter: el $set lleva todos los campos, para que el documento del upsert sea válido.
                new Document("$set", new Document("rol", "Turista").append("estadoCuenta", "Activo")
                        .append("version", 5L).append("actualizadoEn", new java.util.Date())),
                new com.mongodb.client.model.UpdateOptions().upsert(true)))
                .isInstanceOf(MongoWriteException.class)
                .satisfies(e -> assertThat(((MongoWriteException) e).getError().getCode()).isEqualTo(11000));
        assertThat(cuentas.find(Filters.eq("_id", id)).first().getString("estadoCuenta")).isEqualTo("Desactivado");
        cuentas.deleteOne(Filters.eq("_id", id));
    }

    @Test
    void m004CreaElIndiceDeServiciosPorPrestadorEnVolumenNuevoYExistente() {
        for (MongoDatabase base : List.of(nuevo, existente)) {
            assertThat(base.getCollection("servicios").listIndexes().into(new java.util.ArrayList<>()))
                    .anySatisfy(indice -> {
                        assertThat(indice.getString("name")).isEqualTo("prestador");
                        assertThat(indice.get("key", Document.class)).isEqualTo(new Document("prestadorId", 1));
                    });
        }
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
        assertThat(segunda.getStdout()).contains("ya aplicada: M001", "ya aplicada: M002", "ya aplicada: M003",
                "ya aplicada: M004", "ya aplicada: M005", "ya aplicada: M006", "ya aplicada: M007");
        assertThat(documentos(existente, "servicios")).isEqualTo(antes);
        assertThat(idsMigraciones(existente)).containsExactlyElementsOf(MIGRACIONES);
    }

    // ------------------------------------------------------------------ convergencia

    @ParameterizedTest
    @ValueSource(strings = {"servicios", "atractivos", "faq", "avisos"})
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

        assertThat(idsMigraciones(errores)).containsExactly("M001__unidades-alojamiento", "M003__cuentas",
                "M004__indice-servicios-prestador", "M005__faq-avisos", "M007__favoritos-historial");
        assertThat(errores.getCollection("faq").countDocuments()).isZero();
        assertThat(unidadesDe(errores, "Hotel Casa Tayrona")).hasSize(1);
    }

    // ------------------------------------------------------------------ auxiliares

    private static Document cuenta(String id, String rol, String estado, Object version) {
        return new Document("_id", id).append("rol", rol).append("estadoCuenta", estado)
                .append("version", version).append("actualizadoEn", new java.util.Date());
    }

    private static void assertRechazado(MongoCollection<Document> coleccion, Document documento) {
        assertThatThrownBy(() -> coleccion.insertOne(documento))
                .isInstanceOf(MongoWriteException.class)
                .satisfies(e -> assertThat(((MongoWriteException) e).getError().getCode())
                        .isEqualTo(VALIDACION_DOCUMENTO_FALLIDA));
    }

    private static Document opciones(MongoDatabase base, String coleccion) {
        return base.listCollections().filter(Filters.eq("name", coleccion)).first().get("options", Document.class);
    }

    private static List<String> idsMigraciones(MongoDatabase base) {
        List<String> ids = new ArrayList<>();
        base.getCollection("migraciones").find().sort(Sorts.ascending("_id"))
                .forEach(migracion -> ids.add(migracion.getString("_id")));
        return ids;
    }

    private static Document pregunta(String titulo, String cuerpo) {
        return new Document("_id", new ObjectId()).append("titulo", titulo).append("cuerpo", cuerpo)
                .append("fechaActualizacion", new java.util.Date());
    }

    private static Document favorito(String turista, String entidad, String tipoEntidad) {
        return new Document("_id", new ObjectId()).append("turistaId", turista).append("entidadId", entidad)
                .append("tipoEntidad", tipoEntidad).append("fechaCreacion", new java.util.Date());
    }

    private static Document busqueda(String turista, String criterios, String clave) {
        return new Document("_id", new ObjectId()).append("turistaId", turista).append("criteriosBusqueda", criterios)
                .append("fecha", new java.util.Date()).append("clave", clave);
    }

    /** Nombre, claves y si es único (el {@code unique} solo aparece en los índices únicos). */
    private static List<Document> indicesConUnicidad(MongoDatabase base, String coleccion) {
        List<Document> indices = new ArrayList<>();
        base.getCollection(coleccion).listIndexes().forEach(indice -> indices.add(
                new Document("name", indice.get("name")).append("key", indice.get("key"))
                        .append("unique", Boolean.TRUE.equals(indice.getBoolean("unique")))));
        indices.sort((a, b) -> a.getString("name").compareTo(b.getString("name")));
        return indices;
    }

    private static Document aviso(String mensaje, String tipo, boolean activo) {
        return new Document("_id", new ObjectId()).append("mensaje", mensaje).append("tipo", tipo)
                .append("activo", activo).append("fechaPublicacion", new java.util.Date());
    }

    /** Inserta y borra: true si el validador de faq acepta el texto como título. */
    private static boolean aceptaComoTitulo(String titulo) {
        MongoCollection<Document> faq = nuevo.getCollection("faq");
        Document documento = pregunta(titulo, "Cuerpo");
        try {
            faq.insertOne(documento);
        } catch (MongoWriteException e) {
            assertThat(e.getError().getCode()).isEqualTo(VALIDACION_DOCUMENTO_FALLIDA);
            return false;
        }
        faq.deleteOne(Filters.eq("_id", documento.getObjectId("_id")));
        return true;
    }

    private static List<Document> documentos(MongoDatabase base, String coleccion) {
        String nombre = switch (coleccion) {
            case "servicios" -> "nombreComercial";
            case "faq" -> "titulo";
            case "avisos" -> "mensaje";
            default -> "nombre";
        };
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
