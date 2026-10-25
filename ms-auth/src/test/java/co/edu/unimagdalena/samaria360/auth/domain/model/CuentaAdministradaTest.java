package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.EstadoCambiadoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Transiciones de «Administrar cuentas» (D4, DEC-43) y el estado derivado como función total (RF28, RF22, RF62). */
class CuentaAdministradaTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-0000000000aa");
    private static final String RNT = "RNT-12345";

    private static CuentaAdministrada prestador(EstadoValidacion validacion, EstadoCuenta cuenta, long version) {
        return new CuentaAdministrada(ID, Rol.Prestador, cuenta, validacion, version, RNT);
    }

    private static CuentaAdministrada turista(EstadoCuenta cuenta, long version) {
        return new CuentaAdministrada(ID, Rol.Turista, cuenta, null, version, null);
    }

    /** Las 9 combinaciones de validación × cuenta, con la precedencia de AJ2. */
    static Stream<Arguments> combinacionesDePrestador() {
        return Stream.of(
                Arguments.of(EstadoValidacion.Pendiente, EstadoCuenta.Activo, EstadoAdministrativo.Pendiente),
                Arguments.of(EstadoValidacion.Pendiente, EstadoCuenta.Inactivo, EstadoAdministrativo.Pendiente),
                Arguments.of(EstadoValidacion.Pendiente, EstadoCuenta.Desactivado, EstadoAdministrativo.Pendiente),
                Arguments.of(EstadoValidacion.Rechazado, EstadoCuenta.Activo, EstadoAdministrativo.Rechazado),
                Arguments.of(EstadoValidacion.Rechazado, EstadoCuenta.Inactivo, EstadoAdministrativo.Rechazado),
                Arguments.of(EstadoValidacion.Rechazado, EstadoCuenta.Desactivado, EstadoAdministrativo.Rechazado),
                Arguments.of(EstadoValidacion.Activo, EstadoCuenta.Activo, EstadoAdministrativo.Activo),
                // El prestador inactivo de la semilla (V3): validación Activo y cuenta Inactivo.
                Arguments.of(EstadoValidacion.Activo, EstadoCuenta.Inactivo, EstadoAdministrativo.Inactivo),
                Arguments.of(EstadoValidacion.Activo, EstadoCuenta.Desactivado, EstadoAdministrativo.Inactivo));
    }

    @ParameterizedTest
    @MethodSource("combinacionesDePrestador")
    void elEstadoDerivadoDelPrestadorEsTotalYRespetaLaPrecedencia(EstadoValidacion validacion, EstadoCuenta cuenta,
                                                                  EstadoAdministrativo esperado) {
        assertThat(EstadoAdministrativo.dePrestador(validacion, cuenta)).isEqualTo(esperado);
        assertThat(prestador(validacion, cuenta, 0).estado()).isEqualTo(esperado);
    }

    @ParameterizedTest
    @EnumSource(EstadoCuenta.class)
    void unTuristaEstaActivoOEstaDesactivadoYElInactivoHeredadoCuentaComoDesactivado(EstadoCuenta cuenta) {
        EstadoAdministrativo esperado = cuenta == EstadoCuenta.Activo
                ? EstadoAdministrativo.Activo : EstadoAdministrativo.Desactivado;
        assertThat(turista(cuenta, 0).estado()).isEqualTo(esperado);
    }

    @Test
    void aprobarUnPendienteDejaLaValidacionActivaSinTocarLaCuentaYSubeLaVersion() {
        CambioEstadoCuenta cambio = prestador(EstadoValidacion.Pendiente, EstadoCuenta.Activo, 3).aprobar();

        assertThat(cambio.validacionNueva()).isEqualTo(EstadoValidacion.Activo);
        assertThat(cambio.cuentaNueva()).isEqualTo(EstadoCuenta.Activo);
        assertThat(cambio.estadoNuevo()).isEqualTo(EstadoAdministrativo.Activo);
        assertThat(cambio.versionEsperada()).isEqualTo(3);
        assertThat(cambio.versionNueva()).isEqualTo(4);
        assertThat(cambio.cambiaValidacion()).isTrue();
        assertThat(cambio.motivoRechazo()).isNull();
    }

    @Test
    void rechazarUnPendienteGuardaLaJustificacion() {
        CambioEstadoCuenta cambio = prestador(EstadoValidacion.Pendiente, EstadoCuenta.Activo, 0)
                .rechazar(JustificacionRechazo.de("  RNT no vigente  "));

        assertThat(cambio.validacionNueva()).isEqualTo(EstadoValidacion.Rechazado);
        assertThat(cambio.estadoNuevo()).isEqualTo(EstadoAdministrativo.Rechazado);
        assertThat(cambio.motivoRechazo()).isEqualTo("RNT no vigente");
        assertThat(cambio.versionNueva()).isEqualTo(1);
    }

    @Test
    void inactivarUnPrestadorActivoCambiaSoloLaCuentaYSubeLaVersion() {
        CambioEstadoCuenta cambio = prestador(EstadoValidacion.Activo, EstadoCuenta.Activo, 7).inactivar();

        assertThat(cambio.cuentaNueva()).isEqualTo(EstadoCuenta.Inactivo);
        assertThat(cambio.cambiaValidacion()).isFalse();
        assertThat(cambio.estadoNuevo()).isEqualTo(EstadoAdministrativo.Inactivo);
        assertThat(cambio.versionNueva()).isEqualTo(8);
    }

    @Test
    void desactivarYReactivarUnTuristaCambianLaCuentaYSubenLaVersion() {
        CambioEstadoCuenta desactivar = turista(EstadoCuenta.Activo, 0).desactivar();
        assertThat(desactivar.cuentaNueva()).isEqualTo(EstadoCuenta.Desactivado);
        assertThat(desactivar.estadoNuevo()).isEqualTo(EstadoAdministrativo.Desactivado);
        assertThat(desactivar.versionNueva()).isEqualTo(1);
        assertThat(desactivar.validacionNueva()).isNull();

        CambioEstadoCuenta reactivar = turista(EstadoCuenta.Desactivado, 1).reactivar();
        assertThat(reactivar.cuentaNueva()).isEqualTo(EstadoCuenta.Activo);
        assertThat(reactivar.estadoNuevo()).isEqualTo(EstadoAdministrativo.Activo);
        assertThat(reactivar.versionNueva()).isEqualTo(2);
    }

    @Test
    void unTuristaInactivoHeredadoSePuedeReactivarYQuedaConLaCuentaEsperadaReal() {
        CambioEstadoCuenta cambio = turista(EstadoCuenta.Inactivo, 0).reactivar();

        assertThat(cambio.cuentaEsperada()).isEqualTo(EstadoCuenta.Inactivo);
        assertThat(cambio.cuentaNueva()).isEqualTo(EstadoCuenta.Activo);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoValidacion.class, names = {"Activo", "Rechazado"})
    void aprobarORechazarSoloValeParaUnPendiente(EstadoValidacion validacion) {
        CuentaAdministrada cuenta = prestador(validacion, EstadoCuenta.Activo, 2);
        EstadoAdministrativo actual = cuenta.estado();

        assertThatThrownBy(cuenta::aprobar).isInstanceOfSatisfying(EstadoCambiadoException.class,
                e -> assertThat(e.getEstadoActual()).isEqualTo(actual));
        assertThatThrownBy(() -> cuenta.rechazar(JustificacionRechazo.de("x")))
                .isInstanceOf(EstadoCambiadoException.class);
    }

    @Test
    void inactivarSoloValeParaUnPrestadorActivo() {
        assertThatThrownBy(() -> prestador(EstadoValidacion.Pendiente, EstadoCuenta.Activo, 0).inactivar())
                .isInstanceOf(EstadoCambiadoException.class);
        assertThatThrownBy(() -> prestador(EstadoValidacion.Rechazado, EstadoCuenta.Activo, 0).inactivar())
                .isInstanceOf(EstadoCambiadoException.class);
        assertThatThrownBy(() -> prestador(EstadoValidacion.Activo, EstadoCuenta.Inactivo, 0).inactivar())
                .isInstanceOf(EstadoCambiadoException.class);
    }

    @Test
    void unPrestadorInactivoORechazadoNoSeReactiva() {
        // DEC-37: RF22 no define la reactivación; el dominio no ofrece ninguna transición de salida.
        CuentaAdministrada inactivo = prestador(EstadoValidacion.Activo, EstadoCuenta.Inactivo, 4);
        assertThatThrownBy(inactivo::aprobar).isInstanceOf(EstadoCambiadoException.class);
        assertThatThrownBy(inactivo::inactivar).isInstanceOf(EstadoCambiadoException.class);
        assertThatThrownBy(inactivo::reactivar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void desactivarSoloValeParaUnTuristaActivoYReactivarSoloParaUnoDesactivado() {
        assertThatThrownBy(() -> turista(EstadoCuenta.Desactivado, 0).desactivar())
                .isInstanceOf(EstadoCambiadoException.class);
        assertThatThrownBy(() -> turista(EstadoCuenta.Activo, 0).reactivar())
                .isInstanceOf(EstadoCambiadoException.class);
    }

    @Test
    void unaAccionDeOtroRolNoSeAplica() {
        assertThatThrownBy(() -> turista(EstadoCuenta.Activo, 0).inactivar())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> prestador(EstadoValidacion.Activo, EstadoCuenta.Activo, 0).desactivar())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void soloSeAdministranTuristasYPrestadoresYElPrestadorLlevaSuValidacion() {
        assertThatThrownBy(() -> new CuentaAdministrada(ID, Rol.Gestor, EstadoCuenta.Activo, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CuentaAdministrada(ID, Rol.Prestador, EstadoCuenta.Activo, null, 0, RNT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CuentaAdministrada(ID, Rol.Turista, EstadoCuenta.Activo,
                EstadoValidacion.Activo, 0, null)).isInstanceOf(IllegalArgumentException.class);
    }

    /** DEC-146: el prestador lleva su RNT y el turista no. */
    @Test
    void elPrestadorLlevaSuRntYElTuristaNo() {
        assertThatThrownBy(() -> new CuentaAdministrada(ID, Rol.Prestador, EstadoCuenta.Activo,
                EstadoValidacion.Activo, 0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CuentaAdministrada(ID, Rol.Prestador, EstadoCuenta.Activo,
                EstadoValidacion.Activo, 0, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CuentaAdministrada(ID, Rol.Turista, EstadoCuenta.Activo, null, 0, RNT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cadaCambioGeneraUnEventoConLaVersionNueva() {
        CambioEstadoCuenta cambio = turista(EstadoCuenta.Activo, 4).desactivar();

        EventoCuentaActualizada evento = EventoCuentaActualizada.de(cambio, java.time.Instant.parse("2030-01-01T00:00:00Z"));

        assertThat(evento.idUsuario()).isEqualTo(ID);
        assertThat(evento.rol()).isEqualTo(Rol.Turista);
        assertThat(evento.estadoCuenta()).isEqualTo(EstadoAdministrativo.Desactivado);
        assertThat(evento.version()).isEqualTo(5);
        assertThat(evento.rnt()).isNull();
    }

    /** DEC-146: aprobar, rechazar e inactivar a un prestador emiten su evento con el RNT. */
    @Test
    void elEventoDeUnPrestadorLlevaSuRnt() {
        java.time.Instant ahora = java.time.Instant.parse("2030-01-01T00:00:00Z");

        for (CambioEstadoCuenta cambio : java.util.List.of(
                prestador(EstadoValidacion.Pendiente, EstadoCuenta.Activo, 0).aprobar(),
                prestador(EstadoValidacion.Pendiente, EstadoCuenta.Activo, 0)
                        .rechazar(JustificacionRechazo.de("RNT vencido")),
                prestador(EstadoValidacion.Activo, EstadoCuenta.Activo, 0).inactivar())) {
            assertThat(EventoCuentaActualizada.de(cambio, ahora).rnt()).isEqualTo(RNT);
        }
    }

    @Test
    void elEventoExigeElRntSoloAlPrestador() {
        java.time.Instant ahora = java.time.Instant.parse("2030-01-01T00:00:00Z");

        assertThatThrownBy(() -> new EventoCuentaActualizada(ID, Rol.Prestador, EstadoAdministrativo.Activo, 1, null,
                ahora)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EventoCuentaActualizada(ID, Rol.Gestor, EstadoAdministrativo.Activo, 1, RNT,
                ahora)).isInstanceOf(IllegalArgumentException.class);
    }
}
