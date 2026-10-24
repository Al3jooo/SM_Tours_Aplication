package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.auth.application.port.out.TokenProviderPort;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;
import co.edu.unimagdalena.samaria360.auth.infrastructure.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Perfil del turista (RF53, DEC-84, DEC-85) de punta a punta: MockMvc + Spring Security + PostgreSQL (Testcontainers).
 * Cada prueba crea su propio turista para no tocar la semilla.
 */
@PruebaIntegracion
class PerfilApiIntegracionTest {

    private static final String PERFIL = "/api/v1/auth/perfil";
    private static final UUID ID_PRESTADOR = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID ID_ADMIN = UUID.fromString("00000000-0000-4000-8000-000000000004");
    private static final String VALIDO = """
            {"primerNombre":"Lucía","segundoNombre":"","primerApellido":"Gómez","segundoApellido":"Ruiz",
             "paisOrigen":"us","categoriasInteres":["Aventura","Naturaleza"]}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TokenProviderPort tokens;

    // ---- consultar y actualizar -------------------------------------------------------------------------------

    @Test
    void elGetDevuelveLosDatosDelTuristaDelTokenConElCorreo() throws Exception {
        UUID id = crearTurista();

        MvcResult resultado = conToken(get(PERFIL), turista(id, 0));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = cuerpo(resultado);
        assertThat(JsonPath.<String>read(cuerpo, "$.correo")).isEqualTo(correo(id));
        assertThat(JsonPath.<String>read(cuerpo, "$.primerNombre")).isEqualTo("Ana");
        assertThat(JsonPath.<String>read(cuerpo, "$.segundoNombre")).isEqualTo("María");
        assertThat(JsonPath.<String>read(cuerpo, "$.paisOrigen")).isEqualTo("CO");
        assertThat(JsonPath.<List<String>>read(cuerpo, "$.categoriasInteres")).containsExactly("Naturaleza", "Cultura");
    }

    @Test
    void unPutValidoPersisteYRespondeElPerfilResultante() throws Exception {
        UUID id = crearTurista();

        MvcResult resultado = conToken(put(PERFIL).contentType(MediaType.APPLICATION_JSON).content(VALIDO),
                turista(id, 0));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = cuerpo(resultado);
        assertThat(JsonPath.<String>read(cuerpo, "$.primerNombre")).isEqualTo("Lucía");
        assertThat(JsonPath.<Object>read(cuerpo, "$.segundoNombre")).isNull();
        assertThat(JsonPath.<String>read(cuerpo, "$.paisOrigen")).isEqualTo("US");
        assertThat(JsonPath.<List<String>>read(cuerpo, "$.categoriasInteres")).containsExactly("Naturaleza", "Aventura");

        Map<String, Object> fila = jdbc.queryForMap("SELECT * FROM turista WHERE id_usuario = ?", id);
        assertThat(fila).containsEntry("primer_nombre", "Lucía").containsEntry("segundo_nombre", null)
                .containsEntry("segundo_apellido", "Ruiz").containsEntry("pais_origen", "US");
        assertThat(categorias(id)).containsExactly("Aventura", "Naturaleza");
        assertThat(cuerpo(conToken(get(PERFIL), turista(id, 0)))).isEqualTo(cuerpo);
    }

    @Test
    void decisionDEC84_unCorreoEnElCuerpoSeIgnoraYNoCambia() throws Exception {
        UUID id = crearTurista();
        String conCorreo = VALIDO.replace("{", "{\"correo\":\"otro@x.com\",");

        MvcResult resultado = conToken(put(PERFIL).contentType(MediaType.APPLICATION_JSON).content(conCorreo),
                turista(id, 0));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.correo")).isEqualTo(correo(id));
        assertThat(jdbc.queryForObject("SELECT correo FROM usuario WHERE id_usuario = ?", String.class, id))
                .isEqualTo(correo(id));
    }

    @Test
    void unPerfilInvalidoEs400ConUnCodigoPorCampoYNoCambiaNada() throws Exception {
        UUID id = crearTurista();
        String invalido = """
                {"primerNombre":" ","primerApellido":"%s","paisOrigen":"ZZ","categoriasInteres":["Cultura","Cultura"]}"""
                .formatted("x".repeat(51));

        MvcResult resultado = conToken(put(PERFIL).contentType(MediaType.APPLICATION_JSON).content(invalido),
                turista(id, 0));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        String cuerpo = cuerpo(resultado);
        assertThat(JsonPath.<String>read(cuerpo, "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        assertThat(JsonPath.<Map<String, String>>read(cuerpo, "$.campos")).containsExactly(
                Map.entry("primerNombre", "REQUERIDO"),
                Map.entry("primerApellido", "LONGITUD_MAXIMA"),
                Map.entry("paisOrigen", "NO_PERMITIDO"),
                Map.entry("categoriasInteres", "DUPLICADO"));
        assertThat(jdbc.queryForObject("SELECT primer_nombre FROM turista WHERE id_usuario = ?", String.class, id))
                .isEqualTo("Ana");
        assertThat(categorias(id)).containsExactly("Cultura", "Naturaleza");
    }

    @Test
    void lasCategoriasFueraDelCatalogoOVaciasSon400() throws Exception {
        UUID id = crearTurista();

        assertThat(campoCategorias(id, "[\"Playa\"]")).isEqualTo("NO_PERMITIDO");
        assertThat(campoCategorias(id, "[]")).isEqualTo("REQUERIDO");
        assertThat(campoCategorias(id, "[\"Naturaleza\",\"Cultura\",\"Aventura\",\"Cultura\"]")).isEqualTo("DUPLICADO");
    }

    @Test
    void unCuerpoQueNoEsJsonEs400DatosInvalidos() throws Exception {
        MvcResult resultado = conToken(put(PERFIL).contentType(MediaType.APPLICATION_JSON).content("{no-json"),
                turista(crearTurista(), 0));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo")).isEqualTo("DATOS_INVALIDOS");
    }

    @Test
    void unTokenDeTuristaSinFilaEnTuristaEs401() throws Exception {
        MvcResult resultado = conToken(get(PERFIL), turista(UUID.randomUUID(), 0));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo")).isEqualTo("NO_AUTENTICADO");
    }

    // ---- acceso -----------------------------------------------------------------------------------------------

    @Test
    void sinTokenEs401() throws Exception {
        for (MockHttpServletRequestBuilder solicitud : List.of(get(PERFIL),
                put(PERFIL).contentType(MediaType.APPLICATION_JSON).content(VALIDO))) {
            MvcResult resultado = mvc.perform(solicitud).andReturn();
            assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
            assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo")).isEqualTo("NO_AUTENTICADO");
        }
    }

    @Test
    void elPrestadorYElAdministradorReciben403() throws Exception {
        for (String token : List.of(token(ID_PRESTADOR, "prestador@samaria360.test", Rol.Prestador, 0),
                token(ID_ADMIN, "admin@samaria360.test", Rol.Administrador, 0))) {
            for (MockHttpServletRequestBuilder solicitud : List.of(get(PERFIL),
                    put(PERFIL).contentType(MediaType.APPLICATION_JSON).content(VALIDO))) {
                MvcResult resultado = conToken(solicitud, token);
                assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
                assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo")).isEqualTo("ACCESO_DENEGADO");
            }
        }
    }

    @Test
    void regresion_meSigueAccesibleParaElPrestadorYElAdministrador() throws Exception {
        assertThat(conToken(get("/api/v1/auth/me"), token(ID_PRESTADOR, "prestador@samaria360.test", Rol.Prestador, 0))
                .getResponse().getStatus()).isEqualTo(200);
        assertThat(conToken(get("/api/v1/auth/me"), token(ID_ADMIN, "admin@samaria360.test", Rol.Administrador, 0))
                .getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void unTokenConVerViejoEs401SesionInvalidada() throws Exception {
        UUID id = crearTurista();
        jdbc.update("UPDATE usuario SET version_cuenta = 2 WHERE id_usuario = ?", id);

        MvcResult resultado = conToken(put(PERFIL).contentType(MediaType.APPLICATION_JSON).content(VALIDO),
                turista(id, 1));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo")).isEqualTo("SESION_INVALIDADA");
        assertThat(jdbc.queryForObject("SELECT primer_nombre FROM turista WHERE id_usuario = ?", String.class, id))
                .isEqualTo("Ana");
    }

    // ---- utilidades -------------------------------------------------------------------------------------------

    private String campoCategorias(UUID id, String categorias) throws Exception {
        String cuerpo = """
                {"primerNombre":"Ana","primerApellido":"Pérez","paisOrigen":"CO","categoriasInteres":%s}"""
                .formatted(categorias);
        MvcResult resultado = conToken(put(PERFIL).contentType(MediaType.APPLICATION_JSON).content(cuerpo),
                turista(id, 0));
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        return JsonPath.read(cuerpo(resultado), "$.campos.categoriasInteres");
    }

    private MvcResult conToken(MockHttpServletRequestBuilder solicitud, String token) throws Exception {
        return mvc.perform(solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn();
    }

    private String turista(UUID id, long version) {
        return token(id, correo(id), Rol.Turista, version);
    }

    private String token(UUID id, String correo, Rol rol, long version) {
        return tokens.emitir(new Usuario(id, correo, "x", rol, EstadoCuenta.Activo, 0, null, version), Instant.now())
                .token();
    }

    private List<String> categorias(UUID id) {
        return jdbc.queryForList("SELECT categoria FROM turista_categoria_interes WHERE id_usuario = ?"
                + " ORDER BY categoria", String.class, id);
    }

    private static String correo(UUID id) {
        return id + "@perfil.test";
    }

    private UUID crearTurista() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario (id_usuario, id_rol, correo, contrasena_hash, fecha_registro,"
                + " intentos_fallidos, estado_cuenta) VALUES (?, 1, ?, 'x', CURRENT_DATE, 0, 'Activo')",
                id, correo(id));
        jdbc.update("INSERT INTO turista (id_usuario, primer_nombre, segundo_nombre, primer_apellido, pais_origen,"
                + " acepto_politicas) VALUES (?, 'Ana', 'María', 'Pérez', 'CO', TRUE)", id);
        jdbc.update("INSERT INTO turista_categoria_interes (id_usuario, categoria) VALUES (?, 'Cultura'),"
                + " (?, 'Naturaleza')", id, id);
        return id;
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
