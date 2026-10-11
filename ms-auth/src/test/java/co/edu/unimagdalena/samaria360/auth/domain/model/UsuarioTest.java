package co.edu.unimagdalena.samaria360.auth.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final Instant T0 = Instant.parse("2026-10-10T15:00:00Z");

    @Test
    void creaUnUsuarioValido() {
        Usuario usuario = new Usuario(ID, "turista@samaria360.test", "$2a$10$hash", Rol.Turista,
                EstadoCuenta.Activo, 1, T0);

        assertThat(usuario.getId()).isEqualTo(ID);
        assertThat(usuario.getRol()).isEqualTo(Rol.Turista);
        assertThat(usuario.getIntentosFallidos()).isEqualTo(1);
        assertThat(usuario.getBloqueadoHasta()).contains(T0);
    }

    @Test
    void rechazaCamposObligatoriosNulos() {
        assertThatThrownBy(() -> new Usuario(null, "a@b.test", "hash", Rol.Turista, EstadoCuenta.Activo, 0, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Usuario(ID, "a@b.test", "hash", null, EstadoCuenta.Activo, 0, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Usuario(ID, "a@b.test", "hash", Rol.Turista, null, 0, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rechazaCorreoOContrasenaVacios() {
        assertThatThrownBy(() -> new Usuario(ID, " ", "hash", Rol.Turista, EstadoCuenta.Activo, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Usuario(ID, "a@b.test", null, Rol.Turista, EstadoCuenta.Activo, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaIntentosNegativos() {
        assertThatThrownBy(() -> new Usuario(ID, "a@b.test", "hash", Rol.Turista, EstadoCuenta.Activo, -1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unAccesoExitosoLimpiaElBloqueo() {
        Usuario usuario = new Usuario(ID, "a@b.test", "hash", Rol.Turista, EstadoCuenta.Activo, 2, T0);

        usuario.registrarAccesoExitoso();

        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isEmpty();
    }
}
