package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.TokensDePrueba;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * CU «Consultar historial de búsquedas» (RF60; DEC-56, DEC-88) contra MongoDB. Cada prueba usa su propio turista y
 * borra lo que agregó.
 */
@PruebaIntegracion
class HistorialBusquedaApiIntegracionTest {

    private static final String BASE = "/api/v1/catalogo/historial";
    private static final Instant AHORA = Instant.parse("2026-10-23T10:00:00.500Z");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private RelojFijo reloj;

    private final UUID turista = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        reloj.ajustar(AHORA);
    }

    @AfterEach
    void limpiar() {
        historial().deleteMany(Filters.eq("turistaId", turista.toString()));
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

    @Test
    void elPostGuardaLosCriteriosNormalizadosSinUbicacionNiPagina() throws Exception {
        MvcResult resultado = registrar("""
                {"texto":" Playa ","categoria":"Alojamiento","precioMax":200000.00,"usarUbicacion":true,
                 "distanciaKm":5,"accesibilidad":["Visual","MovilidadReducida"],"orden":"Distancia",
                 "serviciosIncluidos":["piscina","WiFi"],"huespedes":2,"lat":11.24,"lng":-74.21,"pagina":3}""");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
        Document guardado = historial().find(Filters.eq("turistaId", turista.toString())).first();
        assertThat(guardado.keySet()).containsExactlyInAnyOrder("_id", "turistaId", "criteriosBusqueda", "fecha",
                "clave");
        assertThat(guardado.getString("criteriosBusqueda")).isEqualTo("{\"texto\":\"Playa\",\"categoria\":"
                + "\"Alojamiento\",\"precioMax\":200000,\"usarUbicacion\":true,\"distanciaKm\":5.0,\"accesibilidad\":"
                + "[\"MovilidadReducida\",\"Visual\"],\"orden\":\"Distancia\",\"serviciosIncluidos\":[\"WiFi\","
                + "\"Piscina\"],\"huespedes\":2}");
        assertThat(guardado.getDate("fecha").toInstant()).isEqualTo(AHORA);

        MvcResult lista = enviar(get(BASE), token(), null);
        assertThat(lista.getResponse().getStatus()).isEqualTo(200);
        Map<String, Object> criterios = leer(lista, "$.elementos[0].criterios");
        assertThat(criterios).containsEntry("texto", "Playa").containsEntry("categoria", "Alojamiento")
                .containsEntry("usarUbicacion", true).containsEntry("distanciaKm", 5.0)
                .containsEntry("orden", "Distancia").containsEntry("huespedes", 2)
                .containsEntry("accesibilidad", List.of("MovilidadReducida", "Visual"))
                .containsEntry("serviciosIncluidos", List.of("WiFi", "Piscina"))
                .doesNotContainKeys("lat", "lng", "pagina");
        assertThat(((Number) criterios.get("precioMax")).intValue()).isEqualTo(200000);
        assertThat(this.<String>leer(lista, "$.elementos[0].fecha")).isEqualTo("2026-10-23T10:00:00.500Z");
        assertThat(this.<String>leer(lista, "$.elementos[0].id")).matches("[0-9a-f]{24}");
    }

    @Test
    void losMismosCriteriosNoSeDuplicanYSubenAlPrimerLugar() throws Exception {
        registrar("{\"texto\":\"Playa\"}");
        reloj.ajustar(AHORA.plusSeconds(60));
        registrar("{\"texto\":\"Tayrona\"}");
        reloj.ajustar(AHORA.plusSeconds(120));

        registrar("{\"texto\":\"  playa \"}");

        MvcResult lista = enviar(get(BASE), token(), null);
        assertThat(this.<List<String>>leer(lista, "$.elementos[*].criterios.texto")).containsExactly("playa", "Tayrona");
        assertThat(historial().countDocuments(Filters.eq("turistaId", turista.toString()))).isEqualTo(2);
    }

    @Test
    void laBusquedaOnceBorraLaMasAntiguaYElGetDevuelveDiezEnOrden() throws Exception {
        for (int i = 1; i <= 11; i++) {
            reloj.ajustar(AHORA.plus(Duration.ofSeconds(i)));
            registrar("{\"texto\":\"busqueda " + i + "\"}");
        }

        List<String> textos = leer(enviar(get(BASE), token(), null), "$.elementos[*].criterios.texto");

        assertThat(textos).hasSize(10).first().isEqualTo("busqueda 11");
        assertThat(textos.get(9)).isEqualTo("busqueda 2");
        assertThat(historial().countDocuments(Filters.eq("turistaId", turista.toString()))).isEqualTo(10);
    }

    @Test
    void unaBusquedaSinCriteriosNoSeGuardaYRespondeIgual204() throws Exception {
        assertThat(registrar("{}").getResponse().getStatus()).isEqualTo(204);
        assertThat(registrar("{\"orden\":\"Nombre\",\"texto\":\"  \"}").getResponse().getStatus()).isEqualTo(204);

        assertThat(historial().countDocuments(Filters.eq("turistaId", turista.toString()))).isZero();
    }

    @Test
    void decisionDEC88_losCriteriosInvalidosSonElMismo400DeLaBusqueda() throws Exception {
        for (String cuerpo : List.of("{\"distanciaKm\":5}", "{\"texto\":\"x\",\"orden\":\"Distancia\"}",
                "{\"precioMax\":-1}", "{\"categoria\":\"Tour\",\"tipoCocina\":\"Caribeña\"}",
                "{\"texto\":\"" + "x".repeat(101) + "\"}")) {
            MvcResult resultado = registrar(cuerpo);
            assertThat(resultado.getResponse().getStatus()).as(cuerpo).isEqualTo(400);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("CRITERIOS_INVALIDOS");
        }
        for (String cuerpo : List.of("{\"categoria\":\"Hotel\"}", "{\"precioMax\":\"caro\"}", "{no-json")) {
            MvcResult resultado = registrar(cuerpo);
            assertThat(resultado.getResponse().getStatus()).as(cuerpo).isEqualTo(400);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        }
        assertThat(historial().countDocuments(Filters.eq("turistaId", turista.toString()))).isZero();
    }

    @Test
    void dosPostSimultaneosDeLaMismaBusquedaDejanUnSoloRegistroYNuncaDan500() throws Exception {
        int solicitudes = 8;
        ExecutorService hilos = Executors.newFixedThreadPool(solicitudes);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Integer>> estados = new ArrayList<>();
            for (int i = 0; i < solicitudes; i++) {
                Callable<Integer> tarea = () -> {
                    salida.await();
                    return registrar("{\"texto\":\"Minca\"}").getResponse().getStatus();
                };
                estados.add(hilos.submit(tarea));
            }
            salida.countDown();
            for (Future<Integer> estado : estados) {
                assertThat(estado.get()).isEqualTo(204);
            }
        } finally {
            hilos.shutdownNow();
        }
        assertThat(historial().countDocuments(Filters.eq("turistaId", turista.toString()))).isEqualTo(1);
    }

    @Test
    void unTuristaNoVeElHistorialDeOtro() throws Exception {
        UUID otro = UUID.randomUUID();
        try {
            enviar(post(BASE), TokensDePrueba.usuario(otro, "Turista", 0), "{\"texto\":\"Playa\"}");

            assertThat(this.<List<Object>>leer(enviar(get(BASE), token(), null), "$.elementos")).isEmpty();
            assertThat(historial().countDocuments(Filters.eq("turistaId", otro.toString()))).isEqualTo(1);
        } finally {
            historial().deleteMany(Filters.eq("turistaId", otro.toString()));
        }
    }

    @Test
    void laBusquedaPublicaSigueSinTokenYNoGuardaHistorial() throws Exception {
        assertThat(mvc.perform(get("/api/v1/catalogo/oferta?texto=Playa")).andReturn().getResponse().getStatus())
                .isEqualTo(200);
        assertThat(enviar(get("/api/v1/catalogo/oferta?texto=Playa"), token(), null).getResponse().getStatus())
                .isEqualTo(200);

        assertThat(historial().countDocuments(Filters.eq("turistaId", turista.toString()))).isZero();
    }

    // ---- acceso -----------------------------------------------------------------------------------------------

    @Test
    void sinTokenEs401() throws Exception {
        for (MockHttpServletRequestBuilder solicitud : List.of(get(BASE),
                post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Playa\"}"))) {
            MvcResult resultado = mvc.perform(solicitud).andReturn();
            assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("NO_AUTENTICADO");
        }
    }

    @Test
    void elPrestadorYElAdministradorReciben403() throws Exception {
        for (String rol : List.of("Prestador", "Administrador")) {
            UUID id = UUID.randomUUID();
            if (rol.equals("Administrador")) {
                filaDeCuenta(id, rol, 1); // sin fila, el rol privilegiado recibiría 401 (DEC-110)
            }
            String token = TokensDePrueba.usuario(id, rol, 1);
            for (MvcResult resultado : List.of(enviar(get(BASE), token, null),
                    enviar(post(BASE), token, "{\"texto\":\"Playa\"}"))) {
                assertThat(resultado.getResponse().getStatus()).as(rol).isEqualTo(403);
                assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("ACCESO_DENEGADO");
            }
            mongo.getCollection("cuentas").deleteOne(Filters.eq("_id", id.toString()));
        }
    }

    @Test
    void unTokenConVerViejoEs401SesionInvalidada() throws Exception {
        mongo.getCollection("cuentas").insertOne(new Document("_id", turista.toString()).append("rol", "Turista")
                .append("estadoCuenta", "Activo").append("version", 3L).append("actualizadoEn", new Date()));
        String viejo = TokensDePrueba.usuario(turista, "Turista", 2);

        for (MvcResult resultado : List.of(enviar(get(BASE), viejo, null),
                enviar(post(BASE), viejo, "{\"texto\":\"Playa\"}"))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("SESION_INVALIDADA");
        }
        assertThat(historial().countDocuments(Filters.eq("turistaId", turista.toString()))).isZero();
    }

    // ---- utilidades -------------------------------------------------------------------------------------------

    private MongoCollection<Document> historial() {
        return mongo.getCollection("historial_busqueda");
    }

    private String token() {
        return TokensDePrueba.usuario(turista, "Turista", 0);
    }

    private MvcResult registrar(String json) throws Exception {
        return enviar(post(BASE), token(), json);
    }

    private MvcResult enviar(MockHttpServletRequestBuilder solicitud, String token, String json) throws Exception {
        solicitud.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(token));
        if (json != null) {
            solicitud.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(solicitud).andReturn();
    }

    private <T> T leer(MvcResult resultado, String ruta) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), ruta);
    }
}
