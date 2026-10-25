package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.TokensDePrueba;
import com.jayway.jsonpath.JsonPath;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesServicio.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * CU «Aprobar o rechazar servicio» (RF21; DEC-128, DEC-144, DEC-145) y el flujo «Consultar mis servicios» del CU
 * «Registrar servicio» (DEC-129, DEC-143), de punta a punta contra MongoDB, con RF22 sobre un servicio nuevo. Los
 * servicios se registran por la API; cada prueba borra lo que agregó.
 */
@PruebaIntegracion
class AprobacionServicioApiIntegracionTest {

    private static final String PRESTADOR_RUTA = "/api/v1/catalogo/prestador/servicios";
    private static final String ADMIN_RUTA = "/api/v1/catalogo/admin/servicios";
    private static final Instant T0 = Instant.parse("2026-10-24T08:00:00Z");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private RelojFijo reloj;

    private final UUID prestador = UUID.randomUUID();
    private final UUID otroPrestador = UUID.randomUUID();
    private final UUID administrador = UUID.randomUUID();
    private final UUID turista = UUID.randomUUID();
    private final String prefijo = "Aprobación " + UUID.randomUUID() + " ";
    private final List<String> pendientesDeLaSemilla = new ArrayList<>();

    @BeforeEach
    void preparar() {
        reloj.ajustar(T0);
        filaDeCuenta(prestador, "Prestador", "Activo", 1, "RNT-APR-1");
        filaDeCuenta(otroPrestador, "Prestador", "Activo", 1, "RNT-APR-2");
        filaDeCuenta(administrador, "Administrador", "Activo", 1, null);
        filaDeCuenta(turista, "Turista", "Activo", 1, null);
        // La cola es global: los Pendientes de la semilla se apartan para que la prueba controle el orden.
        mongo.getCollection("servicios").find(Filters.eq("estadoPublicacion", "Pendiente"))
                .forEach(d -> pendientesDeLaSemilla.add(d.getObjectId("_id").toHexString()));
        mongo.getCollection("servicios").updateMany(Filters.eq("estadoPublicacion", "Pendiente"),
                new Document("$set", new Document("fechaRegistro", Date.from(T0.plus(Duration.ofDays(365))))));
    }

    @AfterEach
    void limpiar() {
        for (UUID id : List.of(prestador, otroPrestador, administrador, turista)) {
            mongo.getCollection("servicios").deleteMany(Filters.eq("prestadorId", id.toString()));
            mongo.getCollection("cuentas").deleteOne(Filters.eq("_id", id.toString()));
        }
        for (String id : pendientesDeLaSemilla) {
            mongo.getCollection("servicios").updateOne(Filters.eq("_id", new ObjectId(id)),
                    List.of(new Document("$set", new Document("fechaRegistro", new Document("$toDate", "$_id")))));
        }
    }

    // ---- Consultar listado pendiente (include) -----------------------------------------------------------------

    @Test
    void elAdministradorVeLaColaDePendientesDelMasAntiguoAlMasReciente() throws Exception {
        String segundo = registrar(prestador, "B", T0.plusSeconds(60));
        String primero = registrar(otroPrestador, "A", T0);

        String json = cuerpo(admin(get(ADMIN_RUTA)));

        List<String> ids = JsonPath.read(json, "$.elementos[*].id");
        assertThat(ids.subList(0, 2)).containsExactly(primero, segundo);
        assertThat(JsonPath.<Integer>read(json, "$.tamano")).isEqualTo(10);
        assertThat(JsonPath.<String>read(json, "$.elementos[0].rnt")).isEqualTo("RNT-APR-2");
        assertThat(JsonPath.<String>read(json, "$.elementos[0].idPrestador")).isEqualTo(otroPrestador.toString());
        assertThat(JsonPath.<String>read(json, "$.elementos[0].estadoPublicacion")).isEqualTo("Pendiente");
    }

    @Test
    void elDetalleDelAdministradorNoAplicaLaVisibilidadPublicaYTraeElEstadoDeLaCuenta() throws Exception {
        String id = registrar(prestador, "Detalle", T0);

        String json = cuerpo(admin(get(ADMIN_RUTA + "/" + id)));

        assertThat(JsonPath.<String>read(json, "$.estadoPublicacion")).isEqualTo("Pendiente");
        assertThat(JsonPath.<String>read(json, "$.rnt")).isEqualTo("RNT-APR-1");
        assertThat(JsonPath.<String>read(json, "$.estadoCuentaPrestador")).isEqualTo("Activo");
        assertThat(admin(get(ADMIN_RUTA + "/" + new ObjectId())).getResponse().getStatus()).isEqualTo(404);
    }

    // ---- Aprobar o rechazar servicio ---------------------------------------------------------------------------

    @Test
    void aprobarLoPublicaYLoVuelveVisibleEnLaBusqueda() throws Exception {
        String id = registrar(prestador, "Aprobado", T0);
        reloj.ajustar(T0.plusSeconds(3600));

        MvcResult resultado = decidir(id, "Aprobar", null);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String json = cuerpo(resultado);
        assertThat(JsonPath.<String>read(json, "$.estadoPublicacion")).isEqualTo("Publicado");
        assertThat(JsonPath.<String>read(json, "$.revisadoPor")).isEqualTo(administrador.toString());
        assertThat(JsonPath.<String>read(json, "$.fechaRevision")).isEqualTo("2026-10-24T09:00:00Z");
        assertThat(JsonPath.<Object>read(json, "$.motivoRechazo")).isNull();
        assertThat(nombresEnLaOferta(prefijo + "Aprobado")).contains(prefijo + "Aprobado");
        assertThat(mvc.perform(get("/api/v1/catalogo/servicios/" + id)).andReturn().getResponse().getStatus())
                .isEqualTo(200);
    }

    @Test
    void rechazarSinJustificacionEs400YConJustificacionGuardaElMotivo() throws Exception {
        String id = registrar(prestador, "Rechazado", T0);

        MvcResult sinJustificacion = decidir(id, "Rechazar", "   ");
        assertThat(sinJustificacion.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<Map<String, String>>read(cuerpo(sinJustificacion), "$.campos"))
                .containsOnly(Map.entry("justificacion", "REQUERIDO"));
        assertThat(decidir(id, "Rechazar", "x".repeat(501)).getResponse().getStatus()).isEqualTo(400);

        MvcResult rechazado = decidir(id, "Rechazar", "  Las fotos no corresponden al lugar  ");

        assertThat(rechazado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<String>read(cuerpo(rechazado), "$.estadoPublicacion")).isEqualTo("Rechazado");
        assertThat(JsonPath.<String>read(cuerpo(rechazado), "$.motivoRechazo"))
                .isEqualTo("Las fotos no corresponden al lugar");
        assertThat(nombresEnLaOferta(prefijo + "Rechazado")).isEmpty();
    }

    @Test
    void unaSegundaDecisionEs409ConElEstadoActual() throws Exception {
        String id = registrar(prestador, "Doble decisión", T0);
        decidir(id, "Aprobar", null);

        MvcResult segunda = decidir(id, "Rechazar", "Llegó tarde");

        assertThat(segunda.getResponse().getStatus()).isEqualTo(409);
        assertThat(JsonPath.<String>read(cuerpo(segunda), "$.codigo")).isEqualTo("ESTADO_CAMBIADO");
        assertThat(JsonPath.<String>read(cuerpo(segunda), "$.estadoActual")).isEqualTo("Publicado");
    }

    @Test
    void unIdInexistenteOInvalidoEs404ServicioNoEncontrado() throws Exception {
        for (String id : List.of(new ObjectId().toHexString(), "no-es-un-id")) {
            MvcResult resultado = decidir(id, "Aprobar", null);
            assertThat(resultado.getResponse().getStatus()).as(id).isEqualTo(404);
            assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo")).isEqualTo("SERVICIO_NO_ENCONTRADO");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Turista", "Prestador"})
    void soloElAdministradorRevisa(String rol) throws Exception {
        String id = registrar(prestador, "Solo admin", T0);
        UUID usuario = "Turista".equals(rol) ? turista : prestador;
        String token = TokensDePrueba.bearer(TokensDePrueba.usuario(usuario, rol, 1));

        for (MockHttpServletRequestBuilder solicitud : List.of(get(ADMIN_RUTA), get(ADMIN_RUTA + "/" + id),
                post(ADMIN_RUTA + "/" + id + "/aprobacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"Aprobar\"}"))) {
            MvcResult resultado = mvc.perform(solicitud.header(HttpHeaders.AUTHORIZATION, token)).andReturn();
            assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
            assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo")).isEqualTo("ACCESO_DENEGADO");
        }
        assertThat(mvc.perform(get(ADMIN_RUTA)).andReturn().getResponse().getStatus()).isEqualTo(401);
    }

    /** RF22 sobre un servicio nuevo: inactivar al prestador oculta también el servicio que registró y se aprobó. */
    @Test
    void inactivarAlPrestadorOcultaSuServicioNuevoPublicado() throws Exception {
        String id = registrar(prestador, "Inactivado", T0);
        decidir(id, "Aprobar", null);
        assertThat(nombresEnLaOferta(prefijo + "Inactivado")).contains(prefijo + "Inactivado");

        MvcResult relevo = mvc.perform(put("/internal/v1/cuentas/" + prestador)
                .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(TokensDePrueba.servicio("ms-catalog")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rol\":\"Prestador\",\"estadoCuenta\":\"Inactivo\",\"version\":2,\"rnt\":\"RNT-APR-1\"}"))
                .andReturn();

        assertThat(relevo.getResponse().getStatus()).isEqualTo(204);
        assertThat(nombresEnLaOferta(prefijo + "Inactivado")).isEmpty();
        assertThat(mvc.perform(get("/api/v1/catalogo/servicios/" + id)).andReturn().getResponse().getStatus())
                .isEqualTo(404);
    }

    // ---- Consultar mis servicios (DEC-129, DEC-143) ------------------------------------------------------------

    @Test
    void elPrestadorVeSoloSusServiciosConElEstadoYElMotivoDelRechazo() throws Exception {
        String rechazado = registrar(prestador, "Mío rechazado", T0);
        String pendiente = registrar(prestador, "Mío pendiente", T0.plusSeconds(60));
        String ajeno = registrar(otroPrestador, "Ajeno", T0.plusSeconds(120));
        decidir(rechazado, "Rechazar", "El RNT no corresponde al servicio");

        String todos = cuerpo(delPrestador(get(PRESTADOR_RUTA)));
        String soloRechazados = cuerpo(delPrestador(get(PRESTADOR_RUTA).param("estado", "Rechazado")));

        assertThat(JsonPath.<List<String>>read(todos, "$.elementos[*].id")).containsExactly(pendiente, rechazado);
        assertThat(JsonPath.<Integer>read(todos, "$.totalElementos")).isEqualTo(2);
        assertThat(JsonPath.<List<String>>read(soloRechazados, "$.elementos[*].id")).containsExactly(rechazado);
        assertThat(JsonPath.<String>read(soloRechazados, "$.elementos[0].motivoRechazo"))
                .isEqualTo("El RNT no corresponde al servicio");
        assertThat(JsonPath.<String>read(soloRechazados, "$.elementos[0].estadoPublicacion")).isEqualTo("Rechazado");
        assertThat(delPrestador(get(PRESTADOR_RUTA + "/" + pendiente)).getResponse().getStatus()).isEqualTo(200);
        MvcResult deOtro = delPrestador(get(PRESTADOR_RUTA + "/" + ajeno));
        assertThat(deOtro.getResponse().getStatus()).isEqualTo(404);
        assertThat(JsonPath.<String>read(cuerpo(deOtro), "$.codigo")).isEqualTo("SERVICIO_NO_ENCONTRADO");
    }

    @Test
    void misServiciosPaginaDeADiezYRechazaUnEstadoOPaginaInvalidos() throws Exception {
        for (int i = 0; i < 11; i++) {
            registrar(prestador, "Página " + i, T0.plusSeconds(i));
        }

        assertThat(JsonPath.<List<Object>>read(cuerpo(delPrestador(get(PRESTADOR_RUTA))), "$.elementos")).hasSize(10);
        String segunda = cuerpo(delPrestador(get(PRESTADOR_RUTA).param("pagina", "1")));
        assertThat(JsonPath.<List<String>>read(segunda, "$.elementos[*].nombreComercial"))
                .containsExactly(prefijo + "Página 0");
        assertThat(JsonPath.<Integer>read(segunda, "$.totalPaginas")).isEqualTo(2);
        MvcResult estado = delPrestador(get(PRESTADOR_RUTA).param("estado", "Borrador"));
        assertThat(estado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<Map<String, String>>read(cuerpo(estado), "$.campos"))
                .containsOnly(Map.entry("estado", "NO_PERMITIDO"));
        MvcResult pagina = delPrestador(get(PRESTADOR_RUTA).param("pagina", "abc"));
        assertThat(JsonPath.<Map<String, String>>read(cuerpo(pagina), "$.campos"))
                .containsOnly(Map.entry("pagina", "FORMATO"));
    }

    // ---- auxiliares --------------------------------------------------------------------------------------------

    /** Registra por la API un tour del prestador, con la hora indicada como fecha de registro. */
    private String registrar(UUID dueno, String nombre, Instant fecha) throws Exception {
        reloj.ajustar(fecha);
        MvcResult resultado = mvc.perform(post(PRESTADOR_RUTA)
                .header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(TokensDePrueba.usuario(dueno, "Prestador", 1)))
                .contentType(MediaType.APPLICATION_JSON).content(json(SolicitudesServicio.tour(prefijo + nombre))))
                .andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        return JsonPath.read(cuerpo(resultado), "$.id");
    }

    private MvcResult decidir(String id, String decision, String justificacion) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("decision", decision);
        cuerpo.put("justificacion", justificacion);
        return admin(post(ADMIN_RUTA + "/" + id + "/aprobacion").contentType(MediaType.APPLICATION_JSON)
                .content(json(cuerpo)));
    }

    private MvcResult admin(MockHttpServletRequestBuilder solicitud) throws Exception {
        return mvc.perform(solicitud.header(HttpHeaders.AUTHORIZATION,
                TokensDePrueba.bearer(TokensDePrueba.usuario(administrador, "Administrador", 1)))).andReturn();
    }

    private MvcResult delPrestador(MockHttpServletRequestBuilder solicitud) throws Exception {
        return mvc.perform(solicitud.header(HttpHeaders.AUTHORIZATION,
                TokensDePrueba.bearer(TokensDePrueba.usuario(prestador, "Prestador", 1)))).andReturn();
    }

    private List<String> nombresEnLaOferta(String texto) throws Exception {
        return JsonPath.read(cuerpo(mvc.perform(get("/api/v1/catalogo/oferta").param("texto", texto)).andReturn()),
                "$.elementos[*].nombre");
    }

    private void filaDeCuenta(UUID id, String rol, String estado, long version, String rnt) {
        Document cuenta = new Document("_id", id.toString()).append("rol", rol).append("estadoCuenta", estado)
                .append("version", version).append("actualizadoEn", new Date());
        if (rnt != null) {
            cuenta.append("rnt", rnt);
        }
        mongo.getCollection("cuentas").insertOne(cuenta);
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
