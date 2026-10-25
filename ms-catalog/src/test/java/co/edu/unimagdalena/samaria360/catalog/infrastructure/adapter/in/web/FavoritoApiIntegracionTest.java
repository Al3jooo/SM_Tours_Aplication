package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.TokensDePrueba;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * CU «Guardar favoritos» (RF54; DEC-80, DEC-86) contra MongoDB: el servicio de la prueba es una copia propia de uno de
 * la semilla, para despublicarlo o inactivar a su prestador sin tocar los demás. Cada prueba borra lo que agregó.
 */
@PruebaIntegracion
class FavoritoApiIntegracionTest {

    private static final String BASE = "/api/v1/catalogo/favoritos";
    private static final Instant AHORA = Instant.parse("2026-10-23T10:00:00.250Z");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private RelojFijo reloj;

    private final UUID turista = UUID.randomUUID();
    private final UUID prestador = UUID.randomUUID();
    private String servicio;
    private String atractivo;

    @BeforeEach
    void preparar() {
        reloj.ajustar(AHORA);
        Document modelo = mongo.getCollection("servicios").find(new Document("nombreComercial", "Cabañas El Roble"))
                .first();
        assertThat(modelo).isNotNull();
        Document copia = new Document(modelo);
        ObjectId id = new ObjectId();
        copia.put("_id", id);
        copia.put("nombreComercial", "Cabañas de la prueba de favoritos");
        copia.put("prestadorId", prestador.toString());
        copia.put("prestadorActivo", true);
        copia.put("estadoPublicacion", "Publicado");
        mongo.getCollection("servicios").insertOne(copia);
        servicio = id.toHexString();
        atractivo = mongo.getCollection("atractivos").find(new Document("nombre", "Cascada El Salto")).first()
                .getObjectId("_id").toHexString();
    }

    @AfterEach
    void limpiar() {
        mongo.getCollection("servicios").deleteMany(new Document("prestadorId", prestador.toString()));
        favoritos().deleteMany(Filters.eq("turistaId", turista.toString()));
        mongo.getCollection("cuentas").deleteOne(Filters.eq("_id", turista.toString()));
    }

    /**
     * Fila de la proyección {@code cuentas}: desde RF45, un Gestor o un Administrador sin fila recibe 401 (DEC-110). La
     * borra el {@code @AfterEach} o la misma prueba.
     */
    private void filaDeCuenta(UUID id, String rol, long version) {
        mongo.getCollection("cuentas").insertOne(new Document("_id", id.toString()).append("rol", rol)
                .append("estadoCuenta", "Activo").append("version", version).append("actualizadoEn", new Date()));
    }

    // ---- guardar, listar y quitar -----------------------------------------------------------------------------

    @Test
    void elPutCreaElFavoritoYElRepetidoNoLoDuplica() throws Exception {
        MvcResult creado = enviar(put(BASE + "/Servicio/" + servicio), token());

        assertThat(creado.getResponse().getStatus()).isEqualTo(201);
        assertThat(this.<String>leer(creado, "$.tipo")).isEqualTo("Servicio");
        assertThat(this.<String>leer(creado, "$.id")).isEqualTo(servicio);
        assertThat(this.<String>leer(creado, "$.nombre")).isEqualTo("Cabañas de la prueba de favoritos");
        assertThat(this.<String>leer(creado, "$.categoria")).isEqualTo("Alojamiento");
        assertThat(this.<String>leer(creado, "$.imagen")).startsWith("/imagenes/catalogo/");
        assertThat(this.<String>leer(creado, "$.fechaCreacion")).isEqualTo("2026-10-23T10:00:00.250Z");
        assertThat(this.<Boolean>leer(creado, "$.disponible")).isTrue();

        MvcResult repetido = enviar(put(BASE + "/Servicio/" + servicio), token());
        assertThat(repetido.getResponse().getStatus()).isEqualTo(200);
        assertThat(favoritos().countDocuments(Filters.eq("turistaId", turista.toString()))).isEqualTo(1);

        Document guardado = favoritos().find(Filters.eq("turistaId", turista.toString())).first();
        assertThat(guardado.getString("tipoEntidad")).isEqualTo("servicio");
        assertThat(guardado.getString("entidadId")).isEqualTo(servicio);
        assertThat(guardado.keySet()).containsExactlyInAnyOrder("_id", "turistaId", "entidadId", "tipoEntidad",
                "fechaCreacion");
    }

    @Test
    void elGetListaServiciosYAtractivosDelMasRecienteAlMasAntiguo() throws Exception {
        enviar(put(BASE + "/Servicio/" + servicio), token());
        reloj.ajustar(AHORA.plusSeconds(60));
        enviar(put(BASE + "/Atractivo/" + atractivo), token());

        MvcResult lista = enviar(get(BASE), token());

        assertThat(lista.getResponse().getStatus()).isEqualTo(200);
        assertThat(this.<List<String>>leer(lista, "$.elementos[*].id")).containsExactly(atractivo, servicio);
        assertThat(this.<String>leer(lista, "$.elementos[0].tipo")).isEqualTo("Atractivo");
        assertThat(this.<String>leer(lista, "$.elementos[0].nombre")).isEqualTo("Cascada El Salto");
        assertThat(this.<Boolean>leer(lista, "$.elementos[0].disponible"))
                .as("un atractivo cerrado temporalmente sigue visible (decisión 5)").isTrue();
    }

    @Test
    void elGetMarcaNoDisponibleAlDespublicarElServicioOInactivarAlPrestadorYNoLoBorra() throws Exception {
        enviar(put(BASE + "/Servicio/" + servicio), token());

        actualizarServicio(new Document("estadoPublicacion", "Pendiente"));
        assertThat(this.<Boolean>leer(enviar(get(BASE), token()), "$.elementos[0].disponible")).isFalse();

        actualizarServicio(new Document("estadoPublicacion", "Publicado").append("prestadorActivo", false));
        MvcResult lista = enviar(get(BASE), token());
        assertThat(this.<Boolean>leer(lista, "$.elementos[0].disponible")).isFalse();
        assertThat(this.<String>leer(lista, "$.elementos[0].nombre")).isEqualTo("Cabañas de la prueba de favoritos");

        assertThat(enviar(put(BASE + "/Servicio/" + servicio), token()).getResponse().getStatus())
                .as("ya guardado: 200 aunque ya no esté visible").isEqualTo(200);
        assertThat(favoritos().countDocuments(Filters.eq("turistaId", turista.toString()))).isEqualTo(1);
    }

    @Test
    void elDeleteQuitaElFavoritoYUnoInexistenteTambienEs204() throws Exception {
        enviar(put(BASE + "/Servicio/" + servicio), token());

        assertThat(enviar(delete(BASE + "/Servicio/" + servicio), token()).getResponse().getStatus()).isEqualTo(204);
        assertThat(enviar(delete(BASE + "/Servicio/" + servicio), token()).getResponse().getStatus()).isEqualTo(204);
        assertThat(enviar(delete(BASE + "/Atractivo/no-es-un-id"), token()).getResponse().getStatus()).isEqualTo(204);
        assertThat(this.<List<Object>>leer(enviar(get(BASE), token()), "$.elementos")).isEmpty();
    }

    @Test
    void unaReferenciaInexistenteONoVisibleEs404YUnTipoInvalidoEs400() throws Exception {
        // El validador v3 (M008) exige la justificación a todo servicio Rechazado.
        actualizarServicio(new Document("estadoPublicacion", "Rechazado").append("motivoRechazo", "Prueba"));

        for (String ruta : List.of("/Servicio/" + servicio, "/Servicio/" + new ObjectId().toHexString(),
                "/Servicio/no-es-un-id", "/Atractivo/" + servicio, "/Servicio/" + atractivo)) {
            MvcResult resultado = enviar(put(BASE + ruta), token());
            assertThat(resultado.getResponse().getStatus()).as(ruta).isEqualTo(404);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("OFERTA_NO_DISPONIBLE");
        }
        for (String tipo : List.of("servicio", "Evento")) {
            MvcResult resultado = enviar(put(BASE + "/" + tipo + "/" + atractivo), token());
            assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("DATOS_INVALIDOS");
            assertThat(this.<String>leer(resultado, "$.campos.tipo")).isEqualTo("NO_PERMITIDO");
        }
        assertThat(favoritos().countDocuments(Filters.eq("turistaId", turista.toString()))).isZero();
    }

    @Test
    void conCienFavoritosElSiguienteEs409LimiteFavoritos() throws Exception {
        List<Document> cien = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            cien.add(new Document("_id", new ObjectId()).append("turistaId", turista.toString())
                    .append("entidadId", new ObjectId().toHexString()).append("tipoEntidad", "atractivo")
                    .append("fechaCreacion", new Date()));
        }
        favoritos().insertMany(cien);

        MvcResult resultado = enviar(put(BASE + "/Servicio/" + servicio), token());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("LIMITE_FAVORITOS");
        assertThat(this.<List<Object>>leer(enviar(get(BASE), token()), "$.elementos")).hasSize(100);
    }

    @Test
    void dosPutSimultaneosDelMismoFavoritoNoLoDuplicanNiDan500() throws Exception {
        int solicitudes = 8;
        ExecutorService hilos = Executors.newFixedThreadPool(solicitudes);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Integer>> estados = new ArrayList<>();
            for (int i = 0; i < solicitudes; i++) {
                Callable<Integer> tarea = () -> {
                    salida.await();
                    return enviar(put(BASE + "/Atractivo/" + atractivo), token()).getResponse().getStatus();
                };
                estados.add(hilos.submit(tarea));
            }
            salida.countDown();
            List<Integer> resultados = new ArrayList<>();
            for (Future<Integer> estado : estados) {
                resultados.add(estado.get());
            }
            assertThat(resultados).allMatch(estado -> estado == 200 || estado == 201);
            assertThat(resultados).contains(201);
        } finally {
            hilos.shutdownNow();
        }
        assertThat(favoritos().countDocuments(Filters.eq("turistaId", turista.toString()))).isEqualTo(1);
    }

    @Test
    void unTuristaNoVeLosFavoritosDeOtro() throws Exception {
        UUID otro = UUID.randomUUID();
        try {
            enviar(put(BASE + "/Servicio/" + servicio), TokensDePrueba.usuario(otro, "Turista", 0));

            assertThat(this.<List<Object>>leer(enviar(get(BASE), token()), "$.elementos")).isEmpty();
            assertThat(enviar(delete(BASE + "/Servicio/" + servicio), token()).getResponse().getStatus())
                    .isEqualTo(204);
            assertThat(favoritos().countDocuments(Filters.eq("turistaId", otro.toString()))).isEqualTo(1);
        } finally {
            favoritos().deleteMany(Filters.eq("turistaId", otro.toString()));
        }
    }

    // ---- acceso -----------------------------------------------------------------------------------------------

    @Test
    void sinTokenEs401EnTodasLasRutas() throws Exception {
        for (MockHttpServletRequestBuilder solicitud : rutas()) {
            MvcResult resultado = mvc.perform(solicitud).andReturn();
            assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("NO_AUTENTICADO");
        }
    }

    @Test
    void elPrestadorYElAdministradorReciben403() throws Exception {
        for (String rol : List.of("Prestador", "Administrador", "Gestor")) {
            UUID id = UUID.randomUUID();
            if (!rol.equals("Prestador")) {
                filaDeCuenta(id, rol, 1); // sin fila, el rol privilegiado recibiría 401 (DEC-110)
            }
            for (MockHttpServletRequestBuilder solicitud : rutas()) {
                MvcResult resultado = enviar(solicitud, TokensDePrueba.usuario(id, rol, 1));
                assertThat(resultado.getResponse().getStatus()).as(rol).isEqualTo(403);
                assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("ACCESO_DENEGADO");
            }
            mongo.getCollection("cuentas").deleteOne(Filters.eq("_id", id.toString()));
        }
    }

    @Test
    void unTokenConVerViejoEs401SesionInvalidada() throws Exception {
        mongo.getCollection("cuentas").insertOne(new Document("_id", turista.toString()).append("rol", "Turista")
                .append("estadoCuenta", "Desactivado").append("version", 3L).append("actualizadoEn", new Date()));
        String viejo = TokensDePrueba.usuario(turista, "Turista", 2);

        for (MockHttpServletRequestBuilder solicitud : rutas()) {
            MvcResult resultado = enviar(solicitud, viejo);
            assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("SESION_INVALIDADA");
        }
        assertThat(favoritos().countDocuments(Filters.eq("turistaId", turista.toString()))).isZero();
    }

    // ---- utilidades -------------------------------------------------------------------------------------------

    private List<MockHttpServletRequestBuilder> rutas() {
        return List.of(get(BASE), put(BASE + "/Servicio/" + servicio), delete(BASE + "/Servicio/" + servicio));
    }

    private MongoCollection<Document> favoritos() {
        return mongo.getCollection("favoritos");
    }

    private void actualizarServicio(Document cambios) {
        mongo.getCollection("servicios").updateOne(Filters.eq("_id", new ObjectId(servicio)),
                new Document("$set", cambios));
    }

    private String token() {
        return TokensDePrueba.usuario(turista, "Turista", 0);
    }

    private MvcResult enviar(MockHttpServletRequestBuilder solicitud, String token) throws Exception {
        return mvc.perform(solicitud.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(token))).andReturn();
    }

    private <T> T leer(MvcResult resultado, String ruta) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), ruta);
    }
}
