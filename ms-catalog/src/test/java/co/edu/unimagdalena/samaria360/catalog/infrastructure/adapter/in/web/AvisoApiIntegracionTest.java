package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.TokensDePrueba;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * CU «Crear y publicar aviso general» (RF40) y «Consultar avisos generales» contra MongoDB: el banner público con la
 * semilla M006, la gestión del Administrador (DEC-60, DEC-64, DEC-65) y sus errores. Cada prueba borra lo que agregó.
 */
@PruebaIntegracion
class AvisoApiIntegracionTest {

    private static final String PUBLICA = "/api/v1/catalogo/avisos";
    private static final String ADMIN = "/api/v1/catalogo/admin/avisos";
    private static final Instant AHORA = Instant.parse("2026-10-23T08:00:00.456Z");
    private static final String SEMILLA = "6a1e00000000000000000001";
    private static final String MENSAJE_SEMILLA =
            "El sendero de la Cascada El Salto está cerrado temporalmente por lluvias.";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private RelojFijo reloj;

    private final UUID administrador = UUID.randomUUID();
    private Set<Object> existentes;

    @BeforeEach
    void preparar() {
        reloj.ajustar(AHORA);
        existentes = new HashSet<>();
        avisos().find().forEach(documento -> existentes.add(documento.get("_id")));
        filaDeCuenta(administrador, "Administrador", 1);
    }

    @AfterEach
    void limpiar() {
        avisos().deleteMany(Filters.nin("_id", existentes));
        avisos().updateOne(Filters.eq("_id", new ObjectId(SEMILLA)), new Document("$set", new Document("activo", true)));
        mongo.getCollection("cuentas").deleteOne(Filters.eq("_id", administrador.toString()));
    }

    private MongoCollection<Document> avisos() {
        return mongo.getCollection("avisos");
    }

    private String admin() {
        return TokensDePrueba.usuario(administrador, "Administrador", 1);
    }

    /**
     * Fila de la proyección {@code cuentas}: desde RF45, un Gestor o un Administrador sin fila recibe 401 (DEC-110). La
     * borra el {@code @AfterEach} o la misma prueba.
     */
    private void filaDeCuenta(UUID id, String rol, long version) {
        mongo.getCollection("cuentas").insertOne(new Document("_id", id.toString()).append("rol", rol)
                .append("estadoCuenta", "Activo").append("version", version).append("actualizadoEn", new Date()));
    }

    private static String cuerpo(String mensaje, String tipo) throws Exception {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("mensaje", mensaje);
        campos.put("tipo", tipo);
        return JSON.writeValueAsString(campos);
    }

    /** Un aviso escrito directamente en la colección, con su fecha de publicación. */
    private ObjectId guardar(String mensaje, boolean activo, String fecha) {
        ObjectId id = new ObjectId();
        avisos().insertOne(new Document("_id", id).append("mensaje", mensaje).append("tipo", "Comunicado")
                .append("activo", activo).append("fechaPublicacion", Date.from(Instant.parse(fecha))));
        return id;
    }

    private MvcResult enviar(MockHttpServletRequestBuilder solicitud, String token, String json) throws Exception {
        if (token != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(token));
        }
        if (json != null) {
            solicitud.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(solicitud).andReturn();
    }

    private MvcResult despublicar(String id) throws Exception {
        return enviar(post(ADMIN + "/" + id + "/despublicacion"), admin(), null);
    }

    private <T> T leer(MvcResult resultado, String ruta) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), ruta);
    }

    // ------------------------------------------------------------------ público (banner)

    @Test
    void elBannerSinTokenSoloTraeLosActivosDelMasRecienteAlMasAntiguo() throws Exception {
        guardar("Activo del 20", true, "2026-10-20T10:00:00Z");
        guardar("Inactivo del 23", false, "2026-10-23T10:00:00Z");
        guardar("Activo del 21", true, "2026-10-21T10:00:00Z");

        MvcResult resultado = enviar(get(PUBLICA), null, null);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(this.<List<String>>leer(resultado, "$.elementos[*].mensaje"))
                .containsExactly(MENSAJE_SEMILLA, "Activo del 21", "Activo del 20");
        assertThat(this.<List<Boolean>>leer(resultado, "$.elementos[*].activo")).containsOnly(true);
        assertThat(this.<String>leer(resultado, "$.elementos[0].tipo")).isEqualTo("Cierre");
        assertThat(this.<String>leer(resultado, "$.elementos[0].fechaPublicacion")).isEqualTo("2026-10-22T13:00:00Z");
    }

    // ------------------------------------------------------------------ Administrador

    @Test
    void publicarResponde201ActivoConLaFechaDelReloj() throws Exception {
        MvcResult resultado = enviar(post(ADMIN), admin(), cuerpo("  Lluvias fuertes en la Sierra  ", "Clima"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        String id = leer(resultado, "$.id");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo(ADMIN + "/" + id);
        assertThat(this.<String>leer(resultado, "$.mensaje")).isEqualTo("Lluvias fuertes en la Sierra");
        assertThat(this.<String>leer(resultado, "$.tipo")).isEqualTo("Clima");
        assertThat(this.<Boolean>leer(resultado, "$.activo")).isTrue();
        assertThat(this.<String>leer(resultado, "$.fechaPublicacion")).isEqualTo("2026-10-23T08:00:00.456Z");
        assertThat(this.<List<String>>leer(enviar(get(PUBLICA), null, null), "$.elementos[*].mensaje"))
                .first().isEqualTo("Lluvias fuertes en la Sierra");
        assertThat(avisos().find(Filters.eq("_id", new ObjectId(id))).first().keySet())
                .containsExactly("_id", "mensaje", "tipo", "activo", "fechaPublicacion");
    }

    @Test
    void despublicarResponde200YElAvisoSaleDelBannerYLaSegundaVezEs409() throws Exception {
        MvcResult primera = despublicar(SEMILLA);

        assertThat(primera.getResponse().getStatus()).isEqualTo(200);
        assertThat(this.<Boolean>leer(primera, "$.activo")).isFalse();
        assertThat(this.<String>leer(primera, "$.mensaje")).isEqualTo(MENSAJE_SEMILLA);
        assertThat(this.<List<String>>leer(enviar(get(PUBLICA), null, null), "$.elementos[*].mensaje"))
                .doesNotContain(MENSAJE_SEMILLA);

        MvcResult segunda = despublicar(SEMILLA);

        assertThat(segunda.getResponse().getStatus()).isEqualTo(409);
        assertThat(this.<String>leer(segunda, "$.codigo")).isEqualTo("ESTADO_CAMBIADO");
        assertThat(this.<String>leer(segunda, "$.estadoActual")).isEqualTo("Inactivo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"6a1e0000000000000000ffff", "no-es-un-id"})
    void despublicarUnIdInexistenteOMalFormadoEs404(String id) throws Exception {
        MvcResult resultado = despublicar(id);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("AVISO_NO_ENCONTRADO");
    }

    @Test
    void elListadoDelAdministradorEsPaginadoDe10ConLosInactivos() throws Exception {
        for (int dia = 1; dia <= 11; dia++) {
            guardar("Aviso del día " + dia, dia % 2 == 0, String.format("2026-09-%02dT10:00:00Z", dia));
        }

        MvcResult primera = enviar(get(ADMIN), admin(), null);
        MvcResult segunda = enviar(get(ADMIN).param("pagina", "1"), admin(), null);

        assertThat(primera.getResponse().getStatus()).isEqualTo(200);
        assertThat(this.<List<String>>leer(primera, "$.elementos[*].mensaje")).hasSize(10)
                .startsWith(MENSAJE_SEMILLA, "Aviso del día 11", "Aviso del día 10");
        assertThat(this.<Integer>leer(primera, "$.pagina")).isZero();
        assertThat(this.<Integer>leer(primera, "$.tamano")).isEqualTo(10);
        assertThat(this.<Integer>leer(primera, "$.totalElementos")).isEqualTo(12);
        assertThat(this.<Integer>leer(primera, "$.totalPaginas")).isEqualTo(2);
        assertThat(this.<List<Boolean>>leer(primera, "$.elementos[*].activo")).contains(true, false);
        assertThat(this.<List<String>>leer(segunda, "$.elementos[*].mensaje"))
                .containsExactly("Aviso del día 2", "Aviso del día 1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "abc", "1.5"})
    void unaPaginaInvalidaResponde400ConElCampo(String pagina) throws Exception {
        MvcResult resultado = enviar(get(ADMIN).param("pagina", pagina), admin(), null);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        assertThat(this.<Map<String, String>>leer(resultado, "$.campos")).isEqualTo(Map.of("pagina", "FORMATO"));
    }

    @Test
    void datosInvalidosResponden400ConUnCodigoPorCampo() throws Exception {
        MvcResult vacio = enviar(post(ADMIN), admin(), cuerpo(" ", "Lluvia"));
        MvcResult largo = enviar(post(ADMIN), admin(), cuerpo("m".repeat(301), null));

        assertThat(vacio.getResponse().getStatus()).isEqualTo(400);
        assertThat(this.<String>leer(vacio, "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        assertThat(this.<Map<String, String>>leer(vacio, "$.campos"))
                .isEqualTo(Map.of("mensaje", "REQUERIDO", "tipo", "NO_PERMITIDO"));
        assertThat(this.<Map<String, String>>leer(largo, "$.campos"))
                .isEqualTo(Map.of("mensaje", "LONGITUD_MAXIMA", "tipo", "REQUERIDO"));
        assertThat(avisos().countDocuments()).isEqualTo(existentes.size());
    }

    // ------------------------------------------------------------------ seguridad (DEC-65)

    @Test
    void sinTokenLaGestionResponde401() throws Exception {
        for (MvcResult resultado : List.of(enviar(get(ADMIN), null, null),
                enviar(post(ADMIN), null, cuerpo("Mensaje", "Clima")),
                enviar(post(ADMIN + "/" + SEMILLA + "/despublicacion"), null, null))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("NO_AUTENTICADO");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Turista", "Prestador", "Gestor"})
    void otroRolRecibe403AccesoDenegado(String rol) throws Exception {
        UUID id = UUID.randomUUID();
        String token = TokensDePrueba.usuario(id, rol, 1);
        if (rol.equals("Gestor")) {
            filaDeCuenta(id, rol, 1);
        }

        for (MvcResult resultado : List.of(enviar(get(ADMIN), token, null),
                enviar(post(ADMIN), token, cuerpo("Mensaje", "Clima")),
                enviar(post(ADMIN + "/" + SEMILLA + "/despublicacion"), token, null))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("ACCESO_DENEGADO");
        }
        mongo.getCollection("cuentas").deleteOne(Filters.eq("_id", id.toString()));
        assertThat(avisos().countDocuments(Filters.eq("activo", true))).isPositive();
    }

    @Test
    void unAdministradorConVerViejoRecibe401SesionInvalidada() throws Exception {
        mongo.getCollection("cuentas").updateOne(Filters.eq("_id", administrador.toString()),
                new Document("$set", new Document("version", 3L)));
        String viejo = TokensDePrueba.usuario(administrador, "Administrador", 2);

        for (MvcResult resultado : List.of(enviar(post(ADMIN), viejo, cuerpo("Mensaje", "Clima")),
                enviar(get(PUBLICA), viejo, null))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("SESION_INVALIDADA");
        }
        assertThat(avisos().countDocuments()).isEqualTo(existentes.size());
    }
}
