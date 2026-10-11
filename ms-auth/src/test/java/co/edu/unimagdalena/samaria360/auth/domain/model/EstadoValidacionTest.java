package co.edu.unimagdalena.samaria360.auth.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstadoValidacionTest {

    @ParameterizedTest
    @EnumSource(EstadoValidacion.class)
    void convierteIdaYVueltaConElTextoDeLaBaseDeDatos(EstadoValidacion estado) {
        assertThat(EstadoValidacion.desdeTexto(estado.texto())).isEqualTo(estado);
    }

    @Test
    void losTextosCoincidenConElCheckDeLaTablaPrestador() {
        assertThat(EstadoValidacion.values())
                .extracting(EstadoValidacion::texto)
                .containsExactly("Pendiente", "Activo", "Rechazado");
    }

    @Test
    void rechazaUnTextoDesconocido() {
        assertThatThrownBy(() -> EstadoValidacion.desdeTexto("Pendiente de Validación"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
