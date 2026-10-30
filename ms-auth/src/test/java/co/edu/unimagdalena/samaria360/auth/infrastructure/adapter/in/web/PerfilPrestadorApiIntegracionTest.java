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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Perfil comercial del Prestador (RF43, DEC-188; O16) de punta a punta: MockMvc + Spring Security + PostgreSQL
 * (Testcontainers). Cada prueba crea su propio prestador para no tocar la semilla.
 */
@PruebaIntegracion
class PerfilPrestadorApiIntegracionTest {

    private static final String PERFIL = "/api/v1/auth/prestador/perfil";
    private static final String VALIDO = """
            {"correoCorporativo":" Reservas@Kayak.TEST ","telefono":"+57 311 222 3344"}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TokenProviderPort tokens;

    @Test
    void elGetDevuelveElPerfilConElRntDeSoloLectura() throws Exception {
        UUID id = crearPrestador();

        MvcResult resultado = conToken(get(PERFIL), prestador(id));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(JsonPath.<Map<String, Object>>read(cuerpo(resultado), "$")).containsExactly(
                Map.entry("correo", correo(id)), Map.entry("representanteLegal", "Marta Rojas"),
                Map.entry("razonSocial", "Kayak Taganga S.A.S."), Map.entry("rnt", rnt(id)),
                Map.entry("correoCorporativo", correo(id)), Map.entry("telefono", "+57 300 000 0000"));
    }

    @Test
    void unPutValidoCambiaElContactoPeroNoLaCredencialNiLaVersion() throws Exception {
        UUID id = crearPrestador();
        String conMas = VALIDO.replace("{", "{\"correo\":\"otro@x.com\",\"rnt\":\"RNT-X\",\"razonSocial\":\"Otra\",");

        MvcResult resultado = conToken(put(PERFIL).contentType(MediaType.APPLICATION_JSON).content(conMas),
                prestador(id));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = cuerpo(resultado);
        assertThat(JsonPath.<String>read(cuerpo, "$.correoCorporativo")).isEqualTo("reservas@kayak.test");
        assertThat(JsonPath.<String>read(cuerpo, "$.telefono")).isEqualTo("+57 311 222 3344");
        assertThat(JsonPath.<String>read(cuerpo, "$.correo")).isEqualTo(correo(id));
        assertThat(JsonPath.<String>read(cuerpo, "$.rnt")).isEqualTo(rnt(id));
        assertThat(JsonPath.<String>read(cuerpo, "$.razonSocial")).isEqualTo("Kayak Taganga S.A.S.");
        Map<String, Object> usuario = jdbc.queryForMap(
                "SELECT correo, version_cuenta FROM usuario WHERE id_usuario = ?", id);
        assertThat(usuario).containsEntry("correo", correo(id)).containsEntry("version_cuenta", 0L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_evento WHERE id_usuario = ?", Long.class, id))
                .isZero();
        assertThat(cuerpo(conToken(get(PERFIL), prestador(id)))).isEqualTo(cuerpo);
    }

    /** D2: la credencial no cambia; el inicio de sesión sigue con el correo de acceso, no con el corporativo. */
    @Test
    void elInicioDeSesionSigueConElCorreoDeAcceso() throws Exception {
        UUID id = crearPrestador();
        conToken(put(PERFIL).contentType(MediaType.APPLICATION_JSON).content(VALIDO), prestador(id));

        MvcResult conElCorporativo = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"reservas@kayak.test\",\"contrasena\":\"no-importa\"}")).andReturn();

        assertThat(conElCorporativo.getResponse().getStatus()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT correo FROM usuario WHERE id_usuario = ?", String.class, id))
                .isEqualTo(correo(id));
    }

    @Test
    void unPerfilInvalidoEs400ConUnCodigoPorCampoYNoCambiaNada() throws Exception {
        UUID id = crearPrestador();

        MvcResult resultado = conToken(put(PERFIL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"correoCorporativo\":\"sin-arroba\",\"telefono\":\"" + "1".repeat(21) + "\"}"),
                prestador(id));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        assertThat(JsonPath.<Map<String, String>>read(cuerpo(resultado), "$.campos")).containsExactly(
                Map.entry("correoCorporativo", "FORMATO"), Map.entry("telefono", "LONGITUD_MAXIMA"));
        assertThat(jdbc.queryForObject("SELECT correo_corporativo FROM prestador WHERE id_usuario = ?", String.class,
                id)).isEqualTo(correo(id));
    }

    @Test
    void otroRolRecibe403YSinTokenEs401() throws Exception {
        // Con su fila en usuario: sin ella, el validador de ver respondería 401 antes que el 403 del rol.
        UUID turista = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario (id_usuario, id_rol, correo, contrasena_hash, fecha_registro,"
                + " intentos_fallidos, estado_cuenta) VALUES (?, 1, ?, 'x', CURRENT_DATE, 0, 'Activo')",
                turista, correo(turista));
        for (MockHttpServletRequestBuilder solicitud : List.of(get(PERFIL),
                put(PERFIL).contentType(MediaType.APPLICATION_JSON).content(VALIDO))) {
            assertThat(conToken(solicitud, token(turista, Rol.Turista)).getResponse().getStatus()).isEqualTo(403);
        }
        assertThat(mvc.perform(get(PERFIL)).andReturn().getResponse().getStatus()).isEqualTo(401);
    }

    // ---- utilidades -------------------------------------------------------------------------------------------

    private MvcResult conToken(MockHttpServletRequestBuilder solicitud, String token) throws Exception {
        return mvc.perform(solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn();
    }

    private String prestador(UUID id) {
        return token(id, Rol.Prestador);
    }

    private String token(UUID id, Rol rol) {
        return tokens.emitir(new Usuario(id, correo(id), "x", rol, EstadoCuenta.Activo, 0, null, 0), Instant.now())
                .token();
    }

    private static String correo(UUID id) {
        return id + "@prestador.test";
    }

    private static String rnt(UUID id) {
        return "RNT-" + id.toString().substring(0, 8);
    }

    /** Un prestador Activo y validado: el único que puede iniciar sesión (RF02, RF28). */
    private UUID crearPrestador() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario (id_usuario, id_rol, correo, contrasena_hash, fecha_registro,"
                + " intentos_fallidos, estado_cuenta) VALUES (?, 2, ?, 'x', CURRENT_DATE, 0, 'Activo')",
                id, correo(id));
        jdbc.update("INSERT INTO prestador (id_usuario, razon_social, rnt, telefono, nombre_representante_legal,"
                + " correo_corporativo, estado_validacion, acepto_politicas) VALUES (?, 'Kayak Taganga S.A.S.', ?,"
                + " '+57 300 000 0000', 'Marta Rojas', ?, 'Activo', TRUE)", id, rnt(id), correo(id));
        return id;
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
