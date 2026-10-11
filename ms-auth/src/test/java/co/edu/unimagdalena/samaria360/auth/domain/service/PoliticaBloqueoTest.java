package co.edu.unimagdalena.samaria360.auth.domain.service;

import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaBloqueadaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaInactivaException;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaBloqueoTest {

    private static final Instant T0 = Instant.parse("2026-10-10T15:00:00Z");
    private static final Instant FIN_BLOQUEO = T0.plus(Duration.ofMinutes(15));

    private final PoliticaBloqueo politica = new PoliticaBloqueo();

    @Test
    void dosIntentosFallidosNoBloquean() {
        Usuario usuario = usuarioActivo();

        fallar(usuario, 2, T0);

        assertThat(usuario.getIntentosFallidos()).isEqualTo(2);
        assertThat(usuario.getBloqueadoHasta()).isEmpty();
        assertThatCode(() -> politica.verificarAcceso(usuario, T0)).doesNotThrowAnyException();
    }

    @Test
    void tresIntentosFallidosBloqueanQuinceMinutosYReinicianElConteo() {
        Usuario usuario = usuarioActivo();

        fallar(usuario, 3, T0);

        assertThat(usuario.getBloqueadoHasta()).contains(FIN_BLOQUEO);
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThatThrownBy(() -> politica.verificarAcceso(usuario, T0))
                .isInstanceOf(CuentaBloqueadaException.class)
                .extracting(e -> ((CuentaBloqueadaException) e).getBloqueadoHasta())
                .isEqualTo(FIN_BLOQUEO);
    }

    @Test
    void laCuentaSeDesbloqueaExactamenteALosQuinceMinutos() {
        Usuario usuario = usuarioActivo();
        fallar(usuario, 3, T0);

        assertThatThrownBy(() -> politica.verificarAcceso(usuario, FIN_BLOQUEO.minusNanos(1)))
                .isInstanceOf(CuentaBloqueadaException.class);
        assertThatCode(() -> politica.verificarAcceso(usuario, FIN_BLOQUEO)).doesNotThrowAnyException();
    }

    @Test
    void laCuentaSigueDesbloqueadaDespuesDeLosQuinceMinutosSinProcesoExterno() {
        Usuario usuario = usuarioActivo();
        fallar(usuario, 3, T0);

        Instant despues = FIN_BLOQUEO.plus(Duration.ofHours(2));

        assertThat(usuario.estaBloqueadoEn(despues)).isFalse();
        assertThatCode(() -> politica.verificarAcceso(usuario, despues)).doesNotThrowAnyException();
    }

    @Test
    void unLoginExitosoReiniciaElContador() {
        Usuario usuario = usuarioActivo();
        fallar(usuario, 2, T0);

        politica.registrarLoginExitoso(usuario);
        assertThat(usuario.getIntentosFallidos()).isZero();

        fallar(usuario, 2, T0);
        assertThat(usuario.getBloqueadoHasta()).isEmpty();
        assertThatCode(() -> politica.verificarAcceso(usuario, T0)).doesNotThrowAnyException();
    }

    @Test
    void alDesbloquearseElUsuarioTieneTresIntentosNuevos() {
        Usuario usuario = usuarioActivo();
        fallar(usuario, 3, T0);

        fallar(usuario, 2, FIN_BLOQUEO);
        assertThatCode(() -> politica.verificarAcceso(usuario, FIN_BLOQUEO)).doesNotThrowAnyException();

        fallar(usuario, 1, FIN_BLOQUEO);
        assertThat(usuario.getBloqueadoHasta()).contains(FIN_BLOQUEO.plus(PoliticaBloqueo.DURACION_BLOQUEO));
    }

    @Test
    void mientrasEstaBloqueadaSeRechazaSinSumarIntentosNiExtenderElBloqueo() {
        Usuario usuario = usuarioActivo();
        fallar(usuario, 3, T0);
        Instant duranteBloqueo = T0.plus(Duration.ofMinutes(10));

        assertThatThrownBy(() -> politica.verificarAcceso(usuario, duranteBloqueo))
                .isInstanceOf(CuentaBloqueadaException.class);
        fallar(usuario, 5, duranteBloqueo);

        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).contains(FIN_BLOQUEO);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCuenta.class, names = {"Inactivo", "Desactivado"})
    void unaCuentaNoActivaNoPuedeIniciarSesion(EstadoCuenta estado) {
        Usuario usuario = usuario(estado);

        assertThatThrownBy(() -> politica.verificarAcceso(usuario, T0))
                .isInstanceOf(CuentaInactivaException.class)
                .extracting(e -> ((CuentaInactivaException) e).getEstadoCuenta())
                .isEqualTo(estado);
    }

    @Test
    void unaCuentaActivaSinBloqueoPuedeIniciarSesion() {
        assertThatCode(() -> politica.verificarAcceso(usuarioActivo(), T0)).doesNotThrowAnyException();
    }

    private void fallar(Usuario usuario, int veces, Instant ahora) {
        for (int i = 0; i < veces; i++) {
            politica.registrarIntentoFallido(usuario, ahora);
        }
    }

    private static Usuario usuarioActivo() {
        return usuario(EstadoCuenta.Activo);
    }

    private static Usuario usuario(EstadoCuenta estado) {
        return new Usuario(UUID.randomUUID(), "turista@samaria360.test", "$2a$10$hash", Rol.Turista,
                estado, 0, null);
    }
}
