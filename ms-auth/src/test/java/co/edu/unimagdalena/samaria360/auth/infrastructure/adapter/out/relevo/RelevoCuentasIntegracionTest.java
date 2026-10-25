package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.relevo;

import co.edu.unimagdalena.samaria360.auth.application.port.out.OutboxPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.auth.application.service.RelevarCuentasService;
import co.edu.unimagdalena.samaria360.auth.domain.model.Destino;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoAdministrativo;
import co.edu.unimagdalena.samaria360.auth.domain.model.EventoCuentaActualizada;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import co.edu.unimagdalena.samaria360.auth.domain.service.PoliticaReintento;
import co.edu.unimagdalena.samaria360.auth.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.security.JwtServicioAdapter;
import co.edu.unimagdalena.samaria360.auth.infrastructure.config.RelevoPropiedades;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El relevo del outbox contra PostgreSQL real y dos servicios simulados con el {@code HttpServer} del JDK (ADR-008): la
 * entrega a los dos destinos, el reintento y que un destino caído no bloquee al otro.
 */
@PruebaIntegracion
class RelevoCuentasIntegracionTest {

    private record Recibida(String metodo, String ruta, String autorizacion, String cuerpo) {
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transacciones;

    @Autowired
    private OutboxPort outbox;

    @Autowired
    private JwtServicioAdapter tokensDeServicio;

    @Autowired
    private RelojPort reloj;

    @Autowired
    private RSAKey llaveJwt;

    private HttpServer catalogo;
    private HttpServer reservas;
    private final List<Recibida> aCatalogo = new CopyOnWriteArrayList<>();
    private final List<Recibida> aReservas = new CopyOnWriteArrayList<>();
    private final AtomicInteger estadoReservas = new AtomicInteger(204);
    private RelevarCuentasService relevo;

    @BeforeEach
    void levantarDestinos() throws IOException {
        // Lo pendiente de otras pruebas no debe entregarse a estos destinos simulados.
        jdbc.update("UPDATE outbox_entrega SET entregado_en = now() WHERE entregado_en IS NULL");
        catalogo = servidor(aCatalogo, new AtomicInteger(204));
        reservas = servidor(aReservas, estadoReservas);
        RelevoPropiedades propiedades = new RelevoPropiedades(true, Duration.ofSeconds(1),
                new RelevoPropiedades.Destinos(url(catalogo), url(reservas)));
        relevo = new RelevarCuentasService(outbox,
                new CuentasHttpAdapter(propiedades, tokensDeServicio, reloj), reloj, new PoliticaReintento());
    }

    @AfterEach
    void detenerDestinos() {
        catalogo.stop(0);
        reservas.stop(0);
    }

    @Test
    void entregaAlosDosDestinosConElJwtDeServicioDeCadaUno() throws Exception {
        UUID usuario = crearTurista();
        registrar(usuario, EstadoAdministrativo.Desactivado, 1);

        assertThat(relevo.relevar(Destino.MS_CATALOG)).isEqualTo(1);
        assertThat(relevo.relevar(Destino.MS_BOOKING)).isEqualTo(1);

        for (var par : List.of(java.util.Map.entry(aCatalogo, "ms-catalog"), java.util.Map.entry(aReservas, "ms-booking"))) {
            assertThat(par.getKey()).singleElement().satisfies(recibida -> {
                assertThat(recibida.metodo()).isEqualTo("PUT");
                assertThat(recibida.ruta()).isEqualTo("/internal/v1/cuentas/" + usuario);
                assertThat(JsonPath.<String>read(recibida.cuerpo(), "$.rol")).isEqualTo("Turista");
                assertThat(JsonPath.<String>read(recibida.cuerpo(), "$.estadoCuenta")).isEqualTo("Desactivado");
                assertThat(JsonPath.<Integer>read(recibida.cuerpo(), "$.version")).isEqualTo(1);
                assertThat(recibida.autorizacion()).startsWith("Bearer ");
                JWTClaimsSet claims = claimsFirmados(recibida.autorizacion().substring("Bearer ".length()));
                assertThat(claims.getSubject()).isEqualTo("ms-auth");
                assertThat(claims.getStringClaim("rol")).isEqualTo("SERVICIO");
                assertThat(claims.getAudience()).containsExactly(par.getValue());
                assertThat(claims.getIssuer()).isEqualTo("samaria360-auth");
                assertThat(claims.getExpirationTime().getTime() - claims.getIssueTime().getTime())
                        .isEqualTo(5 * 60 * 1000L);
            });
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_entrega en JOIN outbox_evento e"
                + " ON e.id_evento = en.id_evento WHERE e.id_usuario = ? AND en.entregado_en IS NOT NULL",
                Integer.class, usuario)).isEqualTo(2);
    }

    /** DEC-146: el RNT viaja en la carga del outbox y en el cuerpo solo para un prestador. */
    @Test
    void elCuerpoLlevaElRntSoloSiLaCuentaEsDeUnPrestador() throws Exception {
        UUID prestador = crearTurista();
        UUID turista = crearTurista();
        registrarPrestador(prestador, "RNT-RELEVO-1", 1);
        registrar(turista, EstadoAdministrativo.Activo, 1);

        assertThat(jdbc.queryForObject("SELECT carga->>'rnt' FROM outbox_evento WHERE id_usuario = ?", String.class,
                prestador)).isEqualTo("RNT-RELEVO-1");
        assertThat(jdbc.queryForObject("SELECT jsonb_exists(carga, 'rnt') FROM outbox_evento WHERE id_usuario = ?",
                Boolean.class, turista)).isFalse();

        assertThat(relevo.relevar(Destino.MS_CATALOG)).isEqualTo(2);
        assertThat(relevo.relevar(Destino.MS_BOOKING)).isEqualTo(2);

        for (List<Recibida> recibidas : List.of(aCatalogo, aReservas)) {
            assertThat(recibidas).filteredOn(r -> r.ruta().endsWith(prestador.toString())).singleElement()
                    .satisfies(r -> {
                        assertThat(JsonPath.<String>read(r.cuerpo(), "$.rol")).isEqualTo("Prestador");
                        assertThat(JsonPath.<String>read(r.cuerpo(), "$.rnt")).isEqualTo("RNT-RELEVO-1");
                    });
            assertThat(recibidas).filteredOn(r -> r.ruta().endsWith(turista.toString())).singleElement()
                    .satisfies(r -> assertThat(JsonPath.<java.util.Map<String, Object>>read(r.cuerpo(), "$"))
                            .doesNotContainKey("rnt"));
        }
    }

    @Test
    void unDestinoCaidoSeReintentaYNoBloqueaAlOtro() throws Exception {
        UUID usuario = crearTurista();
        registrar(usuario, EstadoAdministrativo.Desactivado, 1);
        estadoReservas.set(503);

        assertThat(relevo.relevar(Destino.MS_BOOKING)).isZero();
        assertThat(relevo.relevar(Destino.MS_CATALOG)).isEqualTo(1);

        assertThat(aCatalogo).hasSize(1);
        assertThat(aReservas).hasSize(1); // el intento fallido llegó, con 503
        assertThat(entrega(usuario, "ms-catalog").get("entregado_en")).isNotNull();
        var fallida = entrega(usuario, "ms-booking");
        assertThat(fallida.get("entregado_en")).isNull();
        assertThat(fallida.get("intentos")).isEqualTo(1);
        assertThat((String) fallida.get("ultimo_error")).contains("ms-booking").contains("503");
        assertThat(((java.sql.Timestamp) fallida.get("proximo_intento")).toInstant()).isAfter(Instant.now());

        // Antes de que venza la espera no se reintenta.
        assertThat(relevo.relevar(Destino.MS_BOOKING)).isZero();
        assertThat(aReservas).hasSize(1);

        // El destino vuelve y, vencida la espera, la entrega sale.
        estadoReservas.set(204);
        jdbc.update("UPDATE outbox_entrega SET proximo_intento = now() WHERE entregado_en IS NULL");
        assertThat(relevo.relevar(Destino.MS_BOOKING)).isEqualTo(1);
        assertThat(aReservas).hasSize(2);
        assertThat(entrega(usuario, "ms-booking").get("entregado_en")).isNotNull();
    }

    @Test
    void conVariasVersionesPendientesSoloSeEntregaLaUltimaYLasAnterioresQuedanEntregadas() throws Exception {
        UUID usuario = crearTurista();
        registrar(usuario, EstadoAdministrativo.Desactivado, 1);
        registrar(usuario, EstadoAdministrativo.Activo, 2);

        assertThat(relevo.relevar(Destino.MS_CATALOG)).isEqualTo(1);

        assertThat(aCatalogo).singleElement().satisfies(recibida -> {
            assertThat(JsonPath.<Integer>read(recibida.cuerpo(), "$.version")).isEqualTo(2);
            assertThat(JsonPath.<String>read(recibida.cuerpo(), "$.estadoCuenta")).isEqualTo("Activo");
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_entrega en JOIN outbox_evento e"
                + " ON e.id_evento = en.id_evento WHERE e.id_usuario = ? AND en.destino = 'ms-catalog'"
                + " AND en.entregado_en IS NOT NULL", Integer.class, usuario)).isEqualTo(2);
    }

    @Test
    void unaEntregaConArriendoVigenteNoSeReclamaDeNuevoYUnaVencidaSi() {
        UUID usuario = crearTurista();
        registrar(usuario, EstadoAdministrativo.Desactivado, 1);
        Instant ahora = Instant.now().plusSeconds(1);

        assertThat(outbox.reclamar(Destino.MS_CATALOG, ahora, Duration.ofSeconds(30), 50)).hasSize(1);
        assertThat(outbox.reclamar(Destino.MS_CATALOG, ahora, Duration.ofSeconds(30), 50)).isEmpty();
        assertThat(outbox.reclamar(Destino.MS_CATALOG, ahora.plusSeconds(31), Duration.ofSeconds(30), 50)).hasSize(1);
    }

    // ---- ayudas -----------------------------------------------------------------------------------------------

    private JWTClaimsSet claimsFirmados(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            assertThat(jwt.verify(new RSASSAVerifier(llaveJwt.toRSAPublicKey()))).isTrue();
            return jwt.getJWTClaimsSet();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private java.util.Map<String, Object> entrega(UUID usuario, String destino) {
        return jdbc.queryForMap("SELECT en.entregado_en, en.intentos, en.proximo_intento, en.ultimo_error"
                + " FROM outbox_entrega en JOIN outbox_evento e ON e.id_evento = en.id_evento"
                + " WHERE e.id_usuario = ? AND en.destino = ? ORDER BY e.id_evento DESC LIMIT 1", usuario, destino);
    }

    private void registrar(UUID usuario, EstadoAdministrativo estado, long version) {
        new TransactionTemplate(transacciones).executeWithoutResult(t -> outbox.registrar(
                new EventoCuentaActualizada(usuario, Rol.Turista, estado, version, null, Instant.now()),
                List.of(Destino.values())));
    }

    private void registrarPrestador(UUID usuario, String rnt, long version) {
        new TransactionTemplate(transacciones).executeWithoutResult(t -> outbox.registrar(
                new EventoCuentaActualizada(usuario, Rol.Prestador, EstadoAdministrativo.Activo, version, rnt,
                        Instant.now()),
                List.of(Destino.values())));
    }

    private UUID crearTurista() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario (id_usuario, id_rol, correo, contrasena_hash, fecha_registro,"
                + " intentos_fallidos, estado_cuenta) VALUES (?, 1, ?, 'x', CURRENT_DATE, 0, 'Activo')",
                id, "relevo-" + id + "@mail.test");
        return id;
    }

    private static HttpServer servidor(List<Recibida> recibidas, AtomicInteger estado) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            recibidas.add(new Recibida(intercambio.getRequestMethod(), intercambio.getRequestURI().getPath(),
                    intercambio.getRequestHeaders().getFirst("Authorization"), cuerpo));
            intercambio.sendResponseHeaders(estado.get(), -1);
            intercambio.close();
        });
        servidor.start();
        return servidor;
    }

    private static String url(HttpServer servidor) {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }
}
