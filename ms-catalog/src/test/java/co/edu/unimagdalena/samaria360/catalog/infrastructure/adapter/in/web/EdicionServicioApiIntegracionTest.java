package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.TokensDePrueba;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.Decimal128;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesServicio.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * CU «Actualizar tarifas» (RF32; DEC-183 a DEC-186) y la parte del servicio de «Actualizar datos comerciales» (RF43;
 * DEC-188, DEC-189) de punta a punta contra MongoDB con el validador v4 (M011). Los servicios se registran por la API;
 * cada prueba borra lo que agregó.
 */
@PruebaIntegracion
class EdicionServicioApiIntegracionTest {

    private static final String RUTA = "/api/v1/catalogo/prestador/servicios";
    private static final String ADMIN_RUTA = "/api/v1/catalogo/admin/servicios";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private RelojFijo reloj;

    private final UUID prestador = UUID.randomUUID();
    private final UUID otro = UUID.randomUUID();
    private final UUID administrador = UUID.randomUUID();
    private final String nombre = "Servicio de edición " + UUID.randomUUID();

    @BeforeEach
    void preparar() {
        reloj.ajustar(Instant.parse("2026-10-24T15:00:00Z"));
        filaDeCuenta(prestador, "Prestador", "RNT-EDI-1");
        filaDeCuenta(otro, "Prestador", "RNT-EDI-2");
        filaDeCuenta(administrador, "Administrador", null);
    }

    @AfterEach
    void limpiar() {
        for (UUID id : List.of(prestador, otro, administrador)) {
            mongo.getCollection("servicios").deleteMany(Filters.eq("prestadorId", id.toString()));
            mongo.getCollection("cuentas").deleteOne(Filters.eq("_id", id.toString()));
        }
    }

    // ---- RF32 ----------------------------------------------------------------------------------------------------

    @Test
    void elPrestadorCambiaElPrecioDeSuTourPublicadoYElTuristaVeElNuevo() throws Exception {
        String id = registrar(SolicitudesServicio.tour(nombre));
        aprobar(id);

        MvcResult resultado = editar(id, "precio", Map.of("precioBase", 120000, "version", 1), prestador);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String json = cuerpo(resultado);
        assertThat(JsonPath.<Number>read(json, "$.precioBase").doubleValue()).isEqualTo(120000);
        assertThat(JsonPath.<Integer>read(json, "$.version")).isEqualTo(2);
        assertThat(JsonPath.<String>read(json, "$.estadoPublicacion")).isEqualTo("Publicado");
        Document guardado = documento(id);
        assertThat(guardado.get("precioBase")).isEqualTo(Decimal128.parse("120000"));
        assertThat(guardado.get("version")).isEqualTo(2);
        String publico = cuerpo(mvc.perform(get("/api/v1/catalogo/servicios/" + id)).andReturn());
        assertThat(JsonPath.<Number>read(publico, "$.precioBase").doubleValue()).isEqualTo(120000);
    }

    @Test
    void enAlojamientoElDesdeSeRecalculaEnLaMismaEscritura() throws Exception {
        String id = registrar(SolicitudesServicio.alojamiento(nombre, List.of(
                SolicitudesServicio.unidad("Doble", 8, 2, "150000"),
                SolicitudesServicio.unidad("Suite", 2, 4, "320000"))));
        List<String> unidades = JsonPath.read(cuerpo(consultar(id)), "$.detalleCategoria.unidades[*].idUnidad");

        MvcResult resultado = editar(id, "precio", Map.of("precioBase", 1, "version", 0, "unidades", List.of(
                Map.of("idUnidad", unidades.get(0), "precioNoche", 400000),
                Map.of("idUnidad", unidades.get(1), "precioNoche", 290000))), prestador);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String json = cuerpo(resultado);
        assertThat(JsonPath.<Number>read(json, "$.precioBase").doubleValue()).isEqualTo(290000);
        assertThat(JsonPath.<List<String>>read(json, "$.detalleCategoria.unidades[*].idUnidad"))
                .containsExactlyElementsOf(unidades);
        Document guardado = documento(id);
        assertThat(guardado.get("precioBase")).isEqualTo(Decimal128.parse("290000"));
        assertThat(guardado.get("detalleCategoria", Document.class).getList("unidades", Document.class))
                .extracting(u -> u.get("precioNoche"))
                .containsExactly(Decimal128.parse("400000"), Decimal128.parse("290000"));
    }

    /** DEC-183: la oferta es inmutable; por esta vía se ignora (DEC-141). */
    @Test
    void laOfertaNoSePuedeCambiarPorEstaVia() throws Exception {
        String id = registrar(SolicitudesServicio.tour(nombre));
        Document ofertaAntes = documento(id).get("oferta", Document.class);

        MvcResult resultado = editar(id, "precio", Map.of("precioBase", 70000, "version", 0, "oferta",
                Map.of("diasOperacion", List.of("Lunes"), "salidas", List.of("06:00"))), prestador);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(documento(id).get("oferta", Document.class)).isEqualTo(ofertaAntes);
    }

    @Test
    void losCamposInvalidosSonDatosInvalidosConSusNombresJson() throws Exception {
        String id = registrar(SolicitudesServicio.tour(nombre));

        MvcResult resultado = editar(id, "precio", Map.of("precioBase", 0), prestador);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        String json = cuerpo(resultado);
        assertThat(JsonPath.<String>read(json, "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        assertThat(JsonPath.<Map<String, String>>read(json, "$.campos"))
                .containsOnly(Map.entry("version", "REQUERIDO"), Map.entry("precioBase", "VALOR_MINIMO"));
        assertThat(documento(id).get("version")).isEqualTo(0);
    }

    @Test
    void unServicioAjenoOInexistenteEs404YOtroRolEs403() throws Exception {
        String id = registrar(SolicitudesServicio.tour(nombre));
        Map<String, Object> cuerpo = Map.of("precioBase", 70000, "version", 0);

        assertThat(editar(id, "precio", cuerpo, otro).getResponse().getStatus()).isEqualTo(404);
        assertThat(JsonPath.<String>read(cuerpo(editar(new ObjectId().toHexString(), "precio", cuerpo, prestador)),
                "$.codigo")).isEqualTo("SERVICIO_NO_ENCONTRADO");
        MvcResult turista = mvc.perform(put(RUTA + "/" + id + "/precio")
                .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(TokensDePrueba.usuario(UUID.randomUUID(),
                        "Turista", 0)))
                .contentType(MediaType.APPLICATION_JSON).content(json(cuerpo))).andReturn();
        assertThat(turista.getResponse().getStatus()).isEqualTo(403);
        assertThat(documento(id).get("precioBase")).isEqualTo(Decimal128.parse("95000"));
    }

    /** DEC-186: la decisión del Administrador sube la versión; un precio leído antes no la pisa. */
    @Test
    void unaVersionLeidaAntesDeLaAprobacionEs409ConLaVersionActual() throws Exception {
        String id = registrar(SolicitudesServicio.tour(nombre));
        aprobar(id);

        MvcResult resultado = editar(id, "precio", Map.of("precioBase", 70000, "version", 0), prestador);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        String json = cuerpo(resultado);
        assertThat(JsonPath.<String>read(json, "$.codigo")).isEqualTo("SERVICIO_MODIFICADO");
        assertThat(JsonPath.<Integer>read(json, "$.versionActual")).isEqualTo(1);
        assertThat(documento(id).get("precioBase")).isEqualTo(Decimal128.parse("95000"));
    }

    @Test
    void unServicioRechazadoNoSeEdita() throws Exception {
        String id = registrar(SolicitudesServicio.tour(nombre));
        mvc.perform(admin(post(ADMIN_RUTA + "/" + id + "/aprobacion"))
                .content(json(Map.of("decision", "Rechazar", "justificacion", "Faltan datos")))).andReturn();

        MvcResult resultado = editar(id, "datos-comerciales", datosComerciales("Otro nombre", 1), prestador);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.estadoActual")).isEqualTo("Rechazado");
    }

    // ---- RF43 ----------------------------------------------------------------------------------------------------

    @Test
    void elNombreYElContactoNuevosSonLosQueVeElTurista() throws Exception {
        String id = registrar(SolicitudesServicio.tour(nombre));
        aprobar(id);
        String nuevoNombre = nombre + " renovado";

        MvcResult resultado = editar(id, "datos-comerciales", datosComerciales(" " + nuevoNombre + " ", 1),
                prestador);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(cuerpo(resultado), "$.version")).isEqualTo(2);
        String publico = cuerpo(mvc.perform(get("/api/v1/catalogo/servicios/" + id)).andReturn());
        assertThat(JsonPath.<String>read(publico, "$.nombreComercial")).isEqualTo(nuevoNombre);
        assertThat(JsonPath.<String>read(publico, "$.contacto.telefono")).isEqualTo("+57 311 222 3344");
        assertThat(JsonPath.<String>read(publico, "$.contacto.correo")).isEqualTo("atencion@nuevo.test");
    }

    @Test
    void losDatosComercialesInvalidosSonDatosInvalidos() throws Exception {
        String id = registrar(SolicitudesServicio.tour(nombre));

        MvcResult resultado = editar(id, "datos-comerciales", Map.of("nombreComercial", "", "version", 0,
                "contacto", Map.of("telefono", "+57 300", "correo", "sin-arroba")), prestador);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<Map<String, String>>read(cuerpo(resultado), "$.campos")).containsOnly(
                Map.entry("nombreComercial", "REQUERIDO"), Map.entry("contacto.correo", "FORMATO"));
    }

    // ---- auxiliares ------------------------------------------------------------------------------------------

    private static Map<String, Object> datosComerciales(String nombreComercial, int version) {
        return Map.of("nombreComercial", nombreComercial, "version", version,
                "contacto", Map.of("telefono", "+57 311 222 3344", "correo", "atencion@nuevo.test"));
    }

    private String registrar(Map<String, Object> cuerpo) throws Exception {
        MvcResult resultado = mvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, token(prestador))
                .contentType(MediaType.APPLICATION_JSON).content(json(cuerpo))).andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        return JsonPath.read(cuerpo(resultado), "$.id");
    }

    private void aprobar(String id) throws Exception {
        MvcResult resultado = mvc.perform(admin(post(ADMIN_RUTA + "/" + id + "/aprobacion"))
                .content(json(Map.of("decision", "Aprobar")))).andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    private MvcResult consultar(String id) throws Exception {
        return mvc.perform(get(RUTA + "/" + id).header(HttpHeaders.AUTHORIZATION, token(prestador))).andReturn();
    }

    private MvcResult editar(String id, String que, Map<String, Object> cuerpo, UUID quien) throws Exception {
        return mvc.perform(put(RUTA + "/" + id + "/" + que).header(HttpHeaders.AUTHORIZATION, token(quien))
                .contentType(MediaType.APPLICATION_JSON).content(json(cuerpo))).andReturn();
    }

    private MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder solicitud) {
        return solicitud.header(HttpHeaders.AUTHORIZATION,
                        TokensDePrueba.bearer(TokensDePrueba.usuario(administrador, "Administrador", 1)))
                .contentType(MediaType.APPLICATION_JSON);
    }

    private Document documento(String id) {
        return mongo.getCollection("servicios").find(Filters.eq("_id", new ObjectId(id))).first();
    }

    private static String token(UUID id) {
        return TokensDePrueba.bearer(TokensDePrueba.usuario(id, "Prestador", 1));
    }

    private void filaDeCuenta(UUID id, String rol, String rnt) {
        Document cuenta = new Document("_id", id.toString()).append("rol", rol).append("estadoCuenta", "Activo")
                .append("version", 1L).append("actualizadoEn", new Date());
        if (rnt != null) {
            cuenta.append("rnt", rnt);
        }
        mongo.getCollection("cuentas").insertOne(cuenta);
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
