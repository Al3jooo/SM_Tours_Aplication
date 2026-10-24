package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.CambioRolPropioException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaNoElegibleException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.RolCambiadoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas del cambio de rol (RF45; DEC-82, DEC-105): elegibilidad, rol esperado y la propia cuenta. */
class CuentaConRolTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-0000000000c1");
    private static final UUID ADMIN = UUID.fromString("00000000-0000-4000-8000-0000000000ad");

    private static CuentaConRol cuenta(Rol rol, EstadoCuenta estado, boolean rolBase) {
        return new CuentaConRol(ID, rol, estado, rolBase, 4);
    }

    /** Las cinco transiciones de DEC-105. */
    static Stream<Arguments> transicionesValidas() {
        return Stream.of(
                Arguments.of(Rol.Turista, Rol.Gestor),
                Arguments.of(Rol.Turista, Rol.Administrador),
                Arguments.of(Rol.Gestor, Rol.Administrador),
                Arguments.of(Rol.Administrador, Rol.Gestor),
                Arguments.of(Rol.Gestor, Rol.Turista),
                Arguments.of(Rol.Administrador, Rol.Turista));
    }

    @ParameterizedTest
    @MethodSource("transicionesValidas")
    void unaCuentaActivaConRolBaseCambiaYLaVersionEsperadaEsLaLeida(Rol origen, Rol destino) {
        CambioRol cambio = cuenta(origen, EstadoCuenta.Activo, true).cambiarA(destino, origen, ADMIN);

        assertThat(cambio).isEqualTo(new CambioRol(ID, origen, destino, 4));
        assertThat(cambio.versionNueva()).isEqualTo(5);
    }

    @Test
    void laCuentaDeLaSemillaSinFilaDeTuristaSeModificaPeroNoSeRevoca() {
        assertThat(cuenta(Rol.Gestor, EstadoCuenta.Activo, false).cambiarA(Rol.Administrador, Rol.Gestor, ADMIN)
                .rolNuevo()).isEqualTo(Rol.Administrador);

        for (Rol origen : new Rol[]{Rol.Gestor, Rol.Administrador}) {
            assertThatThrownBy(() -> cuenta(origen, EstadoCuenta.Activo, false).cambiarA(Rol.Turista, origen, ADMIN))
                    .isInstanceOfSatisfying(CuentaNoElegibleException.class,
                            e -> assertThat(e.getMotivo()).isEqualTo(MotivoNoElegible.SIN_ROL_BASE));
        }
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"Turista", "Gestor", "Administrador"})
    void elPrestadorNoEsElegibleAunqueElRolEsperadoNoCoincida(Rol rolEsperado) {
        assertThatThrownBy(() -> cuenta(Rol.Prestador, EstadoCuenta.Activo, false)
                .cambiarA(Rol.Gestor, rolEsperado, ADMIN))
                .isInstanceOfSatisfying(CuentaNoElegibleException.class,
                        e -> assertThat(e.getMotivo()).isEqualTo(MotivoNoElegible.PRESTADOR));
    }

    @Test
    void unRolEsperadoDistintoDelRealEsRolCambiadoConElRolActual() {
        assertThatThrownBy(() -> cuenta(Rol.Gestor, EstadoCuenta.Activo, true)
                .cambiarA(Rol.Gestor, Rol.Turista, ADMIN))
                .isInstanceOfSatisfying(RolCambiadoException.class,
                        e -> assertThat(e.getRolActual()).isEqualTo(Rol.Gestor));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCuenta.class, names = {"Desactivado", "Inactivo"})
    void unTuristaQueNoEstaActivoNoRecibeUnRol(EstadoCuenta estado) {
        assertThatThrownBy(() -> cuenta(Rol.Turista, estado, true).cambiarA(Rol.Gestor, Rol.Turista, ADMIN))
                .isInstanceOfSatisfying(CuentaNoElegibleException.class,
                        e -> assertThat(e.getMotivo()).isEqualTo(MotivoNoElegible.DESACTIVADA));
    }

    @Test
    void elAdministradorNoCambiaSuPropiaCuentaYEsLoPrimeroQueSeRevisa() {
        CuentaConRol propia = new CuentaConRol(ADMIN, Rol.Administrador, EstadoCuenta.Activo, false, 1);

        assertThatThrownBy(() -> propia.cambiarA(Rol.Gestor, Rol.Administrador, ADMIN))
                .isInstanceOf(CambioRolPropioException.class);
        // Antes que el rol esperado: aunque la lista esté desactualizada, la respuesta es la de la propia cuenta.
        assertThatThrownBy(() -> propia.cambiarA(Rol.Gestor, Rol.Turista, ADMIN))
                .isInstanceOf(CambioRolPropioException.class);
    }

    @Test
    void unaVersionNegativaNoEsValida() {
        assertThatThrownBy(() -> new CuentaConRol(ID, Rol.Turista, EstadoCuenta.Activo, true, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
