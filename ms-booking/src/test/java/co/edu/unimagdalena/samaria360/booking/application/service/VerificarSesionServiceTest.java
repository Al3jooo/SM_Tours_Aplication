package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.fake.CuentaRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.booking.domain.model.Cuenta;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.booking.domain.model.RolCuenta;
import co.edu.unimagdalena.samaria360.booking.domain.service.PoliticaSesion;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VerificarSesionServiceTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final CuentaRepositoryEnMemoria cuentas = new CuentaRepositoryEnMemoria();
    private final VerificarSesionService servicio = new VerificarSesionService(cuentas, new PoliticaSesion());

    @Test
    void sinFilaEnLaProyeccionAceptaElTokenDeUnTuristaPeroNoElDeUnAdministrador() {
        assertThat(servicio.sesionVigente(ID, 0, "Turista")).isTrue();
        assertThat(servicio.sesionVigente(ID, 0, "Administrador")).isFalse();
    }

    @Test
    void conFilaComparaElVerConLaVersionGuardada() {
        cuentas.agregar(new Cuenta(ID, RolCuenta.Turista, EstadoCuenta.Desactivado, 4));

        assertThat(servicio.sesionVigente(ID, 4, "Turista")).isTrue();
        assertThat(servicio.sesionVigente(ID, 3, "Turista")).isFalse();
        assertThat(servicio.sesionVigente(ID, 0, "Turista")).isFalse();
    }

    @Test
    void unAdministradorConFilaComparaElVerComoCualquierOtro() {
        cuentas.agregar(new Cuenta(ID, RolCuenta.Administrador, EstadoCuenta.Activo, 2));

        assertThat(servicio.sesionVigente(ID, 2, "Administrador")).isTrue();
        assertThat(servicio.sesionVigente(ID, 1, "Administrador")).isFalse();
    }
}
