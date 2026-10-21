package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.fake.CuentaRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.booking.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ResultadoActualizacion;
import co.edu.unimagdalena.samaria360.booking.domain.model.Cuenta;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.booking.domain.model.RolCuenta;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Proyección {@code cuenta} (ADR-008): upsert idempotente por versión, con el reloj del puerto. */
class ActualizarCuentaServiceTest {

    private static final Instant T0 = Instant.parse("2030-01-10T15:00:00Z");
    private static final UUID TURISTA = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final CuentaRepositoryEnMemoria cuentas = new CuentaRepositoryEnMemoria();
    private final ActualizarCuentaService servicio = new ActualizarCuentaService(cuentas, new RelojFijo(T0));

    @Test
    void unaCuentaNuevaSeGuardaConLaHoraDelReloj() {
        ResultadoActualizacion resultado = servicio.actualizar(
                new Cuenta(TURISTA, RolCuenta.Turista, EstadoCuenta.Desactivado, 2));

        assertThat(resultado).isEqualTo(ResultadoActualizacion.APLICADA);
        assertThat(cuentas.buscar(TURISTA)).get().extracting(Cuenta::version).isEqualTo(2L);
        assertThat(cuentas.actualizadoEn(TURISTA)).isEqualTo(T0);
    }

    @Test
    void unaVersionIgualOMenorSeIgnora() {
        servicio.actualizar(new Cuenta(TURISTA, RolCuenta.Turista, EstadoCuenta.Desactivado, 5));

        assertThat(servicio.actualizar(new Cuenta(TURISTA, RolCuenta.Turista, EstadoCuenta.Activo, 5)))
                .isEqualTo(ResultadoActualizacion.IGNORADA);
        assertThat(servicio.actualizar(new Cuenta(TURISTA, RolCuenta.Turista, EstadoCuenta.Activo, 4)))
                .isEqualTo(ResultadoActualizacion.IGNORADA);
        assertThat(cuentas.buscar(TURISTA)).get().extracting(Cuenta::estado).isEqualTo(EstadoCuenta.Desactivado);
    }

    @Test
    void unaVersionMayorReemplazaALaGuardada() {
        servicio.actualizar(new Cuenta(TURISTA, RolCuenta.Turista, EstadoCuenta.Desactivado, 1));

        assertThat(servicio.actualizar(new Cuenta(TURISTA, RolCuenta.Turista, EstadoCuenta.Activo, 2)))
                .isEqualTo(ResultadoActualizacion.APLICADA);
        assertThat(cuentas.buscar(TURISTA)).get().extracting(Cuenta::estado).isEqualTo(EstadoCuenta.Activo);
    }
}
