package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.TokensDePrueba;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.model.Filters;
import org.junit.jupiter.api.AfterEach;
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
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.FIRMA_JPEG;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.datos;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.imagen;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.parteImagen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * C5: {@code /api/v1/catalogo/gestor/**} es solo del Gestor (DEC-220). Otro rol recibe 403 ACCESO_DENEGADO, en cada
 * endpoint; un Gestor sin fila en la proyección {@code cuentas} recibe 401 SESION_INVALIDADA (DEC-110), y un Gestor con
 * fila pero con un token anterior a su versión, también.
 */
@PruebaIntegracion
class AccesoGestorApiIntegracionTest {

    private static final String RUTA = SolicitudesAtractivo.RUTA;
    private static final String ID = "0000000000000000000000aa";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    private final UUID usuario = UUID.randomUUID();

    @AfterEach
    void limpiar() {
        SolicitudesAtractivo.limpiar(mongo.getDb(), usuario);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Turista", "Prestador", "Administrador"})
    void otroRolRecibe403EnCadaEndpointDelGestor(String rol) throws Exception {
        SolicitudesAtractivo.filaDeCuenta(mongo.getDb(), usuario, rol, 1);
        String token = TokensDePrueba.bearer(TokensDePrueba.usuario(usuario, rol, 1));

        for (Supplier<MockHttpServletRequestBuilder> solicitud : endpoints()) {
            MvcResult resultado = mvc.perform(solicitud.get().header(HttpHeaders.AUTHORIZATION, token)).andReturn();
            assertThat(resultado.getResponse().getStatus()).as("%s %s", rol, resultado.getRequest().getRequestURI())
                    .isEqualTo(403);
            assertThat(codigo(resultado)).isEqualTo("ACCESO_DENEGADO");
        }
        assertThat(mongo.getCollection("atractivos").countDocuments(
                Filters.eq("gestorId", usuario.toString()))).isZero();
    }

    /** Turista y Prestador sin fila siguen pasando la sesión (DEC-110): el 403 viene del rol, no de la proyección. */
    @ParameterizedTest
    @ValueSource(strings = {"Turista", "Prestador"})
    void unTuristaOUnPrestadorSinFilaTambienRecibe403(String rol) throws Exception {
        MvcResult resultado = mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION,
                TokensDePrueba.bearer(TokensDePrueba.usuario(usuario, rol, 0)))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        assertThat(codigo(resultado)).isEqualTo("ACCESO_DENEGADO");
    }

    @Test
    void unGestorSinFilaEnCuentasRecibe401SesionInvalidadaEnCadaEndpoint() throws Exception {
        String token = SolicitudesAtractivo.tokenGestor(usuario, 5);

        for (Supplier<MockHttpServletRequestBuilder> solicitud : endpoints()) {
            MvcResult resultado = mvc.perform(solicitud.get().header(HttpHeaders.AUTHORIZATION, token)).andReturn();
            assertThat(resultado.getResponse().getStatus()).as(resultado.getRequest().getRequestURI()).isEqualTo(401);
            assertThat(codigo(resultado)).isEqualTo("SESION_INVALIDADA");
        }
    }

    @Test
    void unGestorConUnTokenAnteriorASuVersionRecibe401YConElVigenteEntra() throws Exception {
        SolicitudesAtractivo.filaDeCuenta(mongo.getDb(), usuario, "Gestor", 3);

        MvcResult viejo = mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION,
                SolicitudesAtractivo.tokenGestor(usuario, 2))).andReturn();
        MvcResult vigente = mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION,
                SolicitudesAtractivo.tokenGestor(usuario, 3))).andReturn();

        assertThat(viejo.getResponse().getStatus()).isEqualTo(401);
        assertThat(codigo(viejo)).isEqualTo("SESION_INVALIDADA");
        assertThat(vigente.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void sinTokenEs401YElDetallePublicoNoLoPide() throws Exception {
        MvcResult sinToken = mvc.perform(get(RUTA)).andReturn();
        MvcResult publico = mvc.perform(get("/api/v1/catalogo/atractivos/" + ID)).andReturn();

        assertThat(sinToken.getResponse().getStatus()).isEqualTo(401);
        assertThat(publico.getResponse().getStatus()).as("404, no 401").isEqualTo(404);
    }

    private static List<Supplier<MockHttpServletRequestBuilder>> endpoints() {
        return List.of(
                () -> get(RUTA),
                () -> get(RUTA + "/" + ID),
                () -> multipart(RUTA).file(SolicitudesAtractivo.parteDatos(datos("Sin acceso")))
                        .file(parteImagen("a.jpg", imagen(FIRMA_JPEG, 10))),
                () -> put(RUTA + "/" + ID).contentType(MediaType.APPLICATION_JSON).content("{}"),
                () -> post(RUTA + "/" + ID + "/estado").contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    private static String codigo(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.codigo");
    }
}
