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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * CU «Gestionar FAQs» (RF26, RF47) contra MongoDB: el listado público con la semilla M006, la gestión del Administrador
 * (DEC-60 a DEC-62, DEC-65) y sus errores. Cada prueba borra las preguntas que agregó.
 */
@PruebaIntegracion
class PreguntaFrecuenteApiIntegracionTest {

    private static final String PUBLICA = "/api/v1/catalogo/faq";
    private static final String ADMIN = "/api/v1/catalogo/admin/faq";
    private static final Instant AHORA = Instant.parse("2026-10-22T15:30:00.123Z");
    private static final String SEMILLA = "6a1f00000000000000000001";
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
        faq().find().forEach(documento -> existentes.add(documento.get("_id")));
    }

    @AfterEach
    void limpiar() {
        faq().deleteMany(Filters.nin("_id", existentes));
        mongo.getCollection("cuentas").deleteOne(Filters.eq("_id", administrador.toString()));
    }

    private MongoCollection<Document> faq() {
        return mongo.getCollection("faq");
    }

    private String admin() {
        return TokensDePrueba.usuario(administrador, "Administrador", 1);
    }

    private static String cuerpo(String titulo, String cuerpo) throws Exception {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("titulo", titulo);
        campos.put("cuerpo", cuerpo);
        return JSON.writeValueAsString(campos);
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

    private MvcResult crear(String titulo, String texto) throws Exception {
        return enviar(post(ADMIN), admin(), cuerpo(titulo, texto));
    }

    private static String texto(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private <T> T leer(MvcResult resultado, String ruta) throws Exception {
        return JsonPath.read(texto(resultado), ruta);
    }

    private List<String> titulosPublicos() throws Exception {
        MvcResult resultado = enviar(get(PUBLICA), null, null);
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        return leer(resultado, "$.elementos[*].titulo");
    }

    // ------------------------------------------------------------------ público

    @Test
    void elListadoPublicoSinTokenResponde200ConLaSemillaPorTitulo() throws Exception {
        MvcResult resultado = enviar(get(PUBLICA), null, null);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(this.<List<String>>leer(resultado, "$.elementos[*].titulo")).containsExactly(
                "¿Cómo encuentro lugares accesibles?",
                "¿Cómo registro mi negocio como prestador?",
                "¿Cómo reservo un servicio?",
                "¿Hasta cuándo puedo cancelar una reserva?",
                "¿Puedo usar Samaria360 en inglés?");
        assertThat(this.<String>leer(resultado, "$.elementos[2].id")).isEqualTo(SEMILLA);
        assertThat(this.<String>leer(resultado, "$.elementos[2].fechaActualizacion"))
                .isEqualTo("2026-10-22T13:00:00Z");
        assertThat(this.<String>leer(resultado, "$.elementos[2].cuerpo")).startsWith("Busca el servicio");
    }

    @Test
    void elOrdenNoDistingueMayusculasNiTildes() throws Exception {
        for (String titulo : List.of("zeta prueba", "Ábaco prueba", "abeja prueba", "Beta prueba")) {
            assertThat(crear(titulo, "Cuerpo").getResponse().getStatus()).isEqualTo(201);
        }

        assertThat(titulosPublicos()).filteredOn(titulo -> titulo.endsWith("prueba"))
                .containsExactly("Ábaco prueba", "abeja prueba", "Beta prueba", "zeta prueba");
    }

    // ------------------------------------------------------------------ Administrador

    @Test
    void crearResponde201ConLocationYLaPreguntaLimpia() throws Exception {
        MvcResult resultado = crear("  ¿Hay parqueadero?  ", " Sí, en la entrada. ");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        String id = leer(resultado, "$.id");
        assertThat(id).matches("[0-9a-f]{24}");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo(ADMIN + "/" + id);
        assertThat(this.<String>leer(resultado, "$.titulo")).isEqualTo("¿Hay parqueadero?");
        assertThat(this.<String>leer(resultado, "$.cuerpo")).isEqualTo("Sí, en la entrada.");
        assertThat(this.<String>leer(resultado, "$.fechaActualizacion")).isEqualTo("2026-10-22T15:30:00.123Z");

        Document guardado = faq().find(Filters.eq("_id", new ObjectId(id))).first();
        assertThat(guardado.getString("titulo")).isEqualTo("¿Hay parqueadero?");
        assertThat(guardado.getDate("fechaActualizacion")).isEqualTo(Date.from(AHORA));
        assertThat(guardado.keySet()).containsExactly("_id", "titulo", "cuerpo", "fechaActualizacion");
    }

    @Test
    void editarResponde200ConLaPreguntaNuevaYRenuevaLaFecha() throws Exception {
        String id = leer(crear("Antes", "Cuerpo viejo"), "$.id");
        reloj.avanzar(Duration.ofHours(2));

        MvcResult resultado = enviar(put(ADMIN + "/" + id), admin(), cuerpo("Después ", "Cuerpo nuevo"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(this.<String>leer(resultado, "$.id")).isEqualTo(id);
        assertThat(this.<String>leer(resultado, "$.titulo")).isEqualTo("Después");
        assertThat(this.<String>leer(resultado, "$.cuerpo")).isEqualTo("Cuerpo nuevo");
        assertThat(this.<String>leer(resultado, "$.fechaActualizacion")).isEqualTo("2026-10-22T17:30:00.123Z");
        assertThat(titulosPublicos()).contains("Después").doesNotContain("Antes");
    }

    @Test
    void eliminarResponde204YDespuesEditarlaOEliminarlaEs404() throws Exception {
        String id = leer(crear("Para borrar", "Cuerpo"), "$.id");

        assertThat(enviar(delete(ADMIN + "/" + id), admin(), null).getResponse().getStatus()).isEqualTo(204);
        assertThat(faq().find(Filters.eq("_id", new ObjectId(id))).first()).isNull();
        assertThat(titulosPublicos()).doesNotContain("Para borrar");

        for (MvcResult resultado : List.of(enviar(put(ADMIN + "/" + id), admin(), cuerpo("Otra vez", "Cuerpo")),
                enviar(delete(ADMIN + "/" + id), admin(), null))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("FAQ_NO_ENCONTRADA");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"6a1f0000000000000000ffff", "no-es-un-id", "6A1F00000000000000000001"})
    void unIdInexistenteOMalFormadoEs404(String id) throws Exception {
        for (MvcResult resultado : List.of(enviar(put(ADMIN + "/" + id), admin(), cuerpo("Título", "Cuerpo")),
                enviar(delete(ADMIN + "/" + id), admin(), null))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("FAQ_NO_ENCONTRADA");
        }
    }

    @Test
    void datosInvalidosResponden400ConUnCodigoPorCampo() throws Exception {
        MvcResult alCrear = crear("   ", "b".repeat(2001));
        MvcResult alEditar = enviar(put(ADMIN + "/" + SEMILLA), admin(), "{}");

        assertThat(alCrear.getResponse().getStatus()).isEqualTo(400);
        assertThat(this.<String>leer(alCrear, "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        assertThat(this.<Map<String, String>>leer(alCrear, "$.campos"))
                .isEqualTo(Map.of("titulo", "REQUERIDO", "cuerpo", "LONGITUD_MAXIMA"));
        assertThat(this.<Map<String, String>>leer(alEditar, "$.campos"))
                .isEqualTo(Map.of("titulo", "REQUERIDO", "cuerpo", "REQUERIDO"));
        assertThat(faq().find(Filters.eq("_id", new ObjectId(SEMILLA))).first().getString("titulo"))
                .isEqualTo("¿Cómo reservo un servicio?");
        assertThat(faq().countDocuments()).isEqualTo(existentes.size());
    }

    @Test
    void unCuerpoQueNoEsJsonResponde400DatosInvalidosSinCampos() throws Exception {
        MvcResult resultado = enviar(post(ADMIN), admin(), "esto no es JSON");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        assertThat(texto(resultado)).doesNotContain("\"campos\"");
    }

    @Test
    void con200PreguntasLaSiguienteCreacionResponde409LimiteFaq() throws Exception {
        List<Document> relleno = new ArrayList<>();
        for (long i = faq().countDocuments(); i < 200; i++) {
            relleno.add(new Document("_id", new ObjectId()).append("titulo", "Relleno " + i).append("cuerpo", "Cuerpo")
                    .append("fechaActualizacion", Date.from(AHORA)));
        }
        faq().insertMany(relleno);

        MvcResult resultado = crear("La 201", "Cuerpo");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("LIMITE_FAQ");
        assertThat(faq().countDocuments()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ seguridad (DEC-65)

    @Test
    void sinTokenLaGestionResponde401() throws Exception {
        for (MvcResult resultado : List.of(enviar(post(ADMIN), null, cuerpo("Título", "Cuerpo")),
                enviar(put(ADMIN + "/" + SEMILLA), null, cuerpo("Título", "Cuerpo")),
                enviar(delete(ADMIN + "/" + SEMILLA), null, null))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("NO_AUTENTICADO");
        }
        assertThat(faq().countDocuments()).isEqualTo(existentes.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Turista", "Prestador", "Gestor"})
    void otroRolRecibe403AccesoDenegado(String rol) throws Exception {
        String token = TokensDePrueba.usuario(UUID.randomUUID(), rol, 0);

        for (MvcResult resultado : List.of(enviar(post(ADMIN), token, cuerpo("Título", "Cuerpo")),
                enviar(delete(ADMIN + "/" + SEMILLA), token, null))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("ACCESO_DENEGADO");
        }
        assertThat(faq().countDocuments()).isEqualTo(existentes.size());
    }

    /**
     * Hoy el Administrador no tiene fila en {@code cuentas} (no hay RF45), así que su {@code ver} no se invalida: la
     * fila se inserta aquí para probar el mecanismo. Una ruta pública con ese token también responde 401.
     */
    @Test
    void unAdministradorConVerViejoRecibe401SesionInvalidadaTambienEnLaRutaPublica() throws Exception {
        mongo.getCollection("cuentas").insertOne(new Document("_id", administrador.toString())
                .append("rol", "Administrador").append("estadoCuenta", "Activo").append("version", 3L)
                .append("actualizadoEn", new Date()));
        String viejo = TokensDePrueba.usuario(administrador, "Administrador", 2);

        for (MvcResult resultado : List.of(enviar(post(ADMIN), viejo, cuerpo("Título", "Cuerpo")),
                enviar(get(PUBLICA), viejo, null))) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
            assertThat(this.<String>leer(resultado, "$.codigo")).isEqualTo("SESION_INVALIDADA");
        }
        assertThat(enviar(post(ADMIN), TokensDePrueba.usuario(administrador, "Administrador", 3),
                cuerpo("Con la versión vigente", "Cuerpo")).getResponse().getStatus()).isEqualTo(201);
    }
}
