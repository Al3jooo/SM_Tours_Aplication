package co.edu.unimagdalena.samaria360.auth.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El cambio de rol (RF45) y su evento CuentaActualizada (DEC-107). */
class CambioRolTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-0000000000c2");
    private static final Instant T0 = Instant.parse("2030-01-10T15:00:00Z");

    @Test
    void soloQuitaUnAdministradorSiElOrigenEsAdministrador() {
        assertThat(new CambioRol(ID, Rol.Administrador, Rol.Gestor, 0).quitaAdministrador()).isTrue();
        assertThat(new CambioRol(ID, Rol.Administrador, Rol.Turista, 0).quitaAdministrador()).isTrue();
        assertThat(new CambioRol(ID, Rol.Gestor, Rol.Administrador, 0).quitaAdministrador()).isFalse();
        assertThat(new CambioRol(ID, Rol.Turista, Rol.Administrador, 0).quitaAdministrador()).isFalse();
    }

    @Test
    void elPrestadorYElMismoRolNoSonUnCambio() {
        assertThatThrownBy(() -> new CambioRol(ID, Rol.Prestador, Rol.Gestor, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CambioRol(ID, Rol.Turista, Rol.Prestador, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CambioRol(ID, Rol.Gestor, Rol.Gestor, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CambioRol(ID, Rol.Turista, Rol.Gestor, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elEventoLlevaElRolNuevoElEstadoActivoYLaVersionNueva() {
        EventoCuentaActualizada evento = EventoCuentaActualizada.de(new CambioRol(ID, Rol.Gestor, Rol.Turista, 6), T0);

        assertThat(evento).isEqualTo(new EventoCuentaActualizada(ID, Rol.Turista, EstadoAdministrativo.Activo, 7, null, T0));
    }
}
