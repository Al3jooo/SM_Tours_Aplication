package co.edu.unimagdalena.samaria360.catalog.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CondicionAccesibilidadTest {

    @Test
    void sonLasTresCondicionesDelSrs() {
        assertThat(CondicionAccesibilidad.values()).extracting(Enum::name)
                .containsExactly("MovilidadReducida", "Visual", "Auditiva");
    }

    @ParameterizedTest
    @EnumSource(CondicionAccesibilidad.class)
    void convierteDesdeElTextoGuardadoEnMongo(CondicionAccesibilidad condicion) {
        assertThat(CondicionAccesibilidad.desdeTexto(condicion.name())).isEqualTo(condicion);
    }

    @ParameterizedTest
    @ValueSource(strings = {"movilidad reducida", "Rampa de acceso", ""})
    void rechazaTextosQueNoSonDelSrs(String texto) {
        assertThatThrownBy(() -> CondicionAccesibilidad.desdeTexto(texto)).isInstanceOf(IllegalArgumentException.class);
    }
}
