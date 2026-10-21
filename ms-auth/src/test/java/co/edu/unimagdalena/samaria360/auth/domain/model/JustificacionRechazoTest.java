package co.edu.unimagdalena.samaria360.auth.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El rechazo exige una justificación de 1 a 500 caracteres sin contar los espacios de los extremos (RF28, DEC-42). */
class JustificacionRechazoTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void unaJustificacionVaciaOEnBlancoEsRequerida(String texto) {
        assertThat(JustificacionRechazo.validar(texto)).contains(MotivoInvalido.REQUERIDO);
        assertThatThrownBy(() -> JustificacionRechazo.de(texto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void conMasDe500CaracteresSuperaLaLongitudMaxima() {
        assertThat(JustificacionRechazo.validar("a".repeat(501))).contains(MotivoInvalido.LONGITUD_MAXIMA);
        assertThatThrownBy(() -> JustificacionRechazo.de("a".repeat(501))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void los500CaracteresExactosSonValidosYLosEspaciosDeLosExtremosNoCuentan() {
        String texto = "a".repeat(500);

        assertThat(JustificacionRechazo.validar("  " + texto + "  ")).isEmpty();
        assertThat(JustificacionRechazo.de("  " + texto + "  ").texto()).isEqualTo(texto);
    }

    @Test
    void unaJustificacionNormalSeConserva() {
        assertThat(JustificacionRechazo.validar("El RNT no está vigente")).isEmpty();
        assertThat(JustificacionRechazo.de("El RNT no está vigente").texto()).isEqualTo("El RNT no está vigente");
    }
}
