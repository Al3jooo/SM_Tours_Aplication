package co.edu.unimagdalena.samaria360.auth.infrastructure.config;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoInicioSesion;
import co.edu.unimagdalena.samaria360.auth.application.port.in.IniciarSesionUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.SesionIniciada;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CredencialesInvalidasException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaBloqueadaException;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import co.edu.unimagdalena.samaria360.auth.domain.service.PoliticaBloqueo;
import co.edu.unimagdalena.samaria360.auth.infrastructure.PruebaIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Caso de uso completo contra PostgreSQL con BCrypt real y la semilla. El estado se lee con JdbcTemplate
 * después de cada llamada, cuando la transacción ya terminó: así se comprueba lo que quedó confirmado.
 */
@PruebaIntegracion
class IniciarSesionIntegracionTest {

    private static final UUID ID_TURISTA = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final String CORREO_TURISTA = "turista@samaria360.test";
    private static final String CONTRASENA_SEMILLA = "Samaria360*";

    @Autowired
    private IniciarSesionUseCase iniciarSesion;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void restablecerEstadoDeLogin() {
        jdbc.update("UPDATE usuario SET intentos_fallidos = 0, bloqueado_hasta = NULL, estado_cuenta = 'Activo'"
                + " WHERE correo <> 'prestador2@samaria360.test'"); // prestador2 es Inactivo por semilla (V3)
    }

    @Test
    void elUseCaseExpuestoEsElDecoradorTransaccional() {
        assertThat(iniciarSesion).isInstanceOf(IniciarSesionTransaccional.class);
    }

    @Test
    void unaContrasenaIncorrectaPersisteElIntentoFallidoPeseALaExcepcion() {
        assertThatThrownBy(() -> iniciarSesion.iniciarSesion(new ComandoInicioSesion(CORREO_TURISTA, "incorrecta")))
                .isInstanceOf(CredencialesInvalidasException.class);

        // Sin noRollbackFor la transacción se revertiría y esto valdría 0.
        assertThat(intentosFallidos()).isEqualTo(1);
    }

    @Test
    void tresFallosPersistenElBloqueo() {
        // Margen de 1 ms: PostgreSQL redondea timestamptz a microsegundos.
        Instant antes = Instant.now().minusMillis(1);

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> iniciarSesion.iniciarSesion(new ComandoInicioSesion(CORREO_TURISTA, "incorrecta")))
                    .isInstanceOf(CredencialesInvalidasException.class);
        }
        assertThatThrownBy(() -> iniciarSesion.iniciarSesion(new ComandoInicioSesion(CORREO_TURISTA, "incorrecta")))
                .isInstanceOf(CuentaBloqueadaException.class);

        Instant despues = Instant.now().plusMillis(1);
        Instant bloqueadoHasta = jdbc.queryForObject(
                "SELECT bloqueado_hasta FROM usuario WHERE id_usuario = ?", Timestamp.class, ID_TURISTA).toInstant();
        assertThat(bloqueadoHasta)
                .isBetween(antes.plus(PoliticaBloqueo.DURACION_BLOQUEO), despues.plus(PoliticaBloqueo.DURACION_BLOQUEO));
        assertThat(intentosFallidos()).isZero();
    }

    @Test
    void conLaContrasenaDeLaSemillaEmiteElTokenYReiniciaElContador() {
        jdbc.update("UPDATE usuario SET intentos_fallidos = 2 WHERE id_usuario = ?", ID_TURISTA);

        SesionIniciada sesion = iniciarSesion.iniciarSesion(new ComandoInicioSesion(CORREO_TURISTA, CONTRASENA_SEMILLA));

        assertThat(jwtDecoder.decode(sesion.token()).getSubject()).isEqualTo(ID_TURISTA.toString());
        assertThat(sesion.idUsuario()).isEqualTo(ID_TURISTA);
        assertThat(sesion.rol()).isEqualTo(Rol.Turista);
        assertThat(intentosFallidos()).isZero();
    }

    @Test
    void unPrestadorPendienteDeValidacionPuedeIniciarSesion() {
        SesionIniciada sesion = iniciarSesion.iniciarSesion(
                new ComandoInicioSesion("prestador@samaria360.test", CONTRASENA_SEMILLA));

        assertThat(sesion.rol()).isEqualTo(Rol.Prestador);
    }

    private int intentosFallidos() {
        return jdbc.queryForObject(
                "SELECT intentos_fallidos FROM usuario WHERE id_usuario = ?", Integer.class, ID_TURISTA);
    }
}
