package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.fake.CuentaRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.application.fake.VisibilidadPrestadorFalsa;
import co.edu.unimagdalena.samaria360.catalog.application.fake.VisibilidadPrestadorFalsa.Llamada;
import co.edu.unimagdalena.samaria360.catalog.application.port.in.ResultadoActualizacion;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Cuenta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.RolCuenta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Proyección {@code cuentas} y derivación de {@code prestadorActivo} (ADR-008, RF22) con dobles de los puertos. */
class ActualizarCuentaServiceTest {

    private static final UUID PRESTADOR = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID TURISTA = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final CuentaRepositoryEnMemoria cuentas = new CuentaRepositoryEnMemoria();
    private final VisibilidadPrestadorFalsa visibilidad = new VisibilidadPrestadorFalsa();
    private final ActualizarCuentaService servicio = new ActualizarCuentaService(cuentas, visibilidad);

    @Test
    void unaCuentaNuevaSeGuardaYAplicaLaVisibilidadDelPrestador() {
        ResultadoActualizacion resultado = servicio.actualizar(
                new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Inactivo, 3));

        assertThat(resultado).isEqualTo(ResultadoActualizacion.APLICADA);
        assertThat(cuentas.buscar(PRESTADOR)).get().extracting(Cuenta::version).isEqualTo(3L);
        assertThat(visibilidad.llamadas()).containsExactly(new Llamada(PRESTADOR.toString(), false));
    }

    @ParameterizedTest
    @EnumSource(EstadoCuenta.class)
    void prestadorActivoSeDerivaDelEstado(EstadoCuenta estado) {
        servicio.actualizar(new Cuenta(PRESTADOR, RolCuenta.Prestador, estado, 1));

        assertThat(visibilidad.llamadas()).containsExactly(
                new Llamada(PRESTADOR.toString(), estado == EstadoCuenta.Activo));
    }

    @Test
    void unaVersionIgualOMenorSeIgnoraSinTocarNada() {
        servicio.actualizar(new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Inactivo, 5));
        visibilidad.llamadas().clear();

        assertThat(servicio.actualizar(new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Activo, 5)))
                .isEqualTo(ResultadoActualizacion.IGNORADA);
        assertThat(servicio.actualizar(new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Activo, 4)))
                .isEqualTo(ResultadoActualizacion.IGNORADA);

        assertThat(cuentas.buscar(PRESTADOR)).get().extracting(Cuenta::estado).isEqualTo(EstadoCuenta.Inactivo);
        assertThat(visibilidad.llamadas()).isEmpty();
    }

    @Test
    void unaVersionMayorLlegadaFueraDeOrdenReemplazaALaGuardada() {
        servicio.actualizar(new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Activo, 2));
        servicio.actualizar(new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Inactivo, 4));

        assertThat(servicio.actualizar(new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Activo, 3)))
                .isEqualTo(ResultadoActualizacion.IGNORADA);
        assertThat(cuentas.buscar(PRESTADOR)).get().extracting(Cuenta::estado).isEqualTo(EstadoCuenta.Inactivo);
    }

    @Test
    void unaCuentaQueNoEsDePrestadorNoTocaLosServicios() {
        servicio.actualizar(new Cuenta(TURISTA, RolCuenta.Turista, EstadoCuenta.Desactivado, 1));

        assertThat(visibilidad.llamadas()).isEmpty();
        assertThat(cuentas.versionDe(TURISTA)).hasValue(1);
    }

    @Test
    void siUnaCuentaDejaDeSerPrestadorSusServiciosSeOcultan() {
        servicio.actualizar(new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Activo, 1));
        visibilidad.llamadas().clear();

        servicio.actualizar(new Cuenta(PRESTADOR, RolCuenta.Gestor, EstadoCuenta.Activo, 2));

        assertThat(visibilidad.llamadas()).containsExactly(new Llamada(PRESTADOR.toString(), false));
    }

    @Test
    void siFallaLaVisibilidadLaProyeccionNoAvanzaYElRelevoPuedeReintentar() {
        visibilidad.fallar(new IllegalStateException("mongo caído"));

        assertThatThrownBy(() -> servicio.actualizar(
                new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Inactivo, 2)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(cuentas.buscar(PRESTADOR)).isEmpty();
    }

    @Test
    void siOtroEnvioGanaLaCarreraAlGuardarSeIgnora() {
        CuentaRepositoryEnMemoria carrera = new CuentaRepositoryEnMemoria() {
            @Override
            public boolean guardarSiEsMasNueva(Cuenta cuenta) {
                return false;
            }
        };

        assertThat(new ActualizarCuentaService(carrera, visibilidad)
                .actualizar(new Cuenta(TURISTA, RolCuenta.Turista, EstadoCuenta.Activo, 2)))
                .isEqualTo(ResultadoActualizacion.IGNORADA);
    }
}
