package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.fake.CuentaRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Cuenta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.RolCuenta;
import co.edu.unimagdalena.samaria360.catalog.domain.service.PoliticaSesion;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VerificarSesionServiceTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final CuentaRepositoryEnMemoria cuentas = new CuentaRepositoryEnMemoria();
    private final VerificarSesionService servicio = new VerificarSesionService(cuentas, new PoliticaSesion());

    @Test
    void sinFilaEnLaProyeccionAceptaElToken() {
        assertThat(servicio.sesionVigente(ID, 0)).isTrue();
    }

    @Test
    void conFilaComparaElVerConLaVersionGuardada() {
        cuentas.agregar(new Cuenta(ID, RolCuenta.Turista, EstadoCuenta.Desactivado, 4));

        assertThat(servicio.sesionVigente(ID, 4)).isTrue();
        assertThat(servicio.sesionVigente(ID, 3)).isFalse();
        assertThat(servicio.sesionVigente(ID, 0)).isFalse();
    }
}
