package co.edu.unimagdalena.samaria360.catalog.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuentaTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000002");

    @ParameterizedTest
    @EnumSource(EstadoCuenta.class)
    void soloUnPrestadorActivoTieneSusServiciosVisibles(EstadoCuenta estado) {
        assertThat(new Cuenta(ID, RolCuenta.Prestador, estado, 1).prestadorActivo())
                .isEqualTo(estado == EstadoCuenta.Activo);
    }

    @ParameterizedTest
    @EnumSource(value = RolCuenta.class, names = {"Turista", "Gestor", "Administrador"})
    void unaCuentaQueNoEsDePrestadorNuncaActivaServicios(RolCuenta rol) {
        assertThat(new Cuenta(ID, rol, EstadoCuenta.Activo, 1).prestadorActivo()).isFalse();
    }

    @Test
    void laVersionEsAlMenosUno() {
        assertThatThrownBy(() -> new Cuenta(ID, RolCuenta.Turista, EstadoCuenta.Activo, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
