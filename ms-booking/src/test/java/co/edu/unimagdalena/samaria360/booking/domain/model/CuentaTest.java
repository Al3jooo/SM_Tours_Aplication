package co.edu.unimagdalena.samaria360.booking.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuentaTest {

    @Test
    void laVersionEsAlMenosUno() {
        assertThatThrownBy(() -> new Cuenta(UUID.randomUUID(), RolCuenta.Turista, EstadoCuenta.Activo, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
