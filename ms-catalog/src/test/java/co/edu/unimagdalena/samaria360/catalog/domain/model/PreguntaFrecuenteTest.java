package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Límites de RF26 y DEC-61: título de 1 a 150 y cuerpo de 1 a 2000, sin los espacios de los extremos. */
class PreguntaFrecuenteTest {

    private static final String ID = "6a1f00000000000000000001";
    private static final Instant AHORA = Instant.parse("2026-10-22T15:00:00Z");
    /** Fuera del plano básico: un punto de código, dos unidades UTF-16. */
    static final String EMOJI = "🌴";

    private static Map<String, MotivoInvalido> motivos(String titulo, String cuerpo) {
        try {
            PreguntaFrecuente.de(ID, titulo, cuerpo, AHORA);
        } catch (ContenidoInvalidoException e) {
            return e.getCampos();
        }
        return Map.of();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n "})
    void unTituloOUnCuerpoVacioOSoloConEspaciosEsRequerido(String vacio) {
        assertThat(motivos(vacio, "Cuerpo")).containsExactly(Map.entry("titulo", MotivoInvalido.REQUERIDO));
        assertThat(motivos("Título", vacio)).containsExactly(Map.entry("cuerpo", MotivoInvalido.REQUERIDO));
    }

    @Test
    void elTituloAdmiteHasta150CaracteresYNoUnoMas() {
        assertThat(motivos("a".repeat(150), "Cuerpo")).isEmpty();
        assertThat(motivos("a".repeat(151), "Cuerpo"))
                .containsExactly(Map.entry("titulo", MotivoInvalido.LONGITUD_MAXIMA));
    }

    @Test
    void elCuerpoAdmiteHasta2000CaracteresYNoUnoMas() {
        assertThat(motivos("Título", "b".repeat(2000))).isEmpty();
        assertThat(motivos("Título", "b".repeat(2001)))
                .containsExactly(Map.entry("cuerpo", MotivoInvalido.LONGITUD_MAXIMA));
    }

    @Test
    void losCaracteresSeCuentanPorPuntoDeCodigoComoEnMongo() {
        assertThat(EMOJI.repeat(150)).hasSize(300);
        assertThat(motivos(EMOJI.repeat(150), "Cuerpo")).isEmpty();
        assertThat(motivos(EMOJI.repeat(151), "Cuerpo"))
                .containsExactly(Map.entry("titulo", MotivoInvalido.LONGITUD_MAXIMA));
    }

    @Test
    void losEspaciosDeLosExtremosNoCuentanYNoSeGuardan() {
        PreguntaFrecuente pregunta = PreguntaFrecuente.de(ID, "  " + "a".repeat(150) + "\n", " Cuerpo\t", AHORA);

        assertThat(pregunta.titulo()).isEqualTo("a".repeat(150));
        assertThat(pregunta.cuerpo()).isEqualTo("Cuerpo");
        assertThat(pregunta.fechaActualizacion()).isEqualTo(AHORA);
    }

    @Test
    void unEspacioDeNoSeparacionNoSeRecortaComoEnElPatternDeMongo() {
        assertThat(PreguntaFrecuente.de(ID, "Hola ", "Cuerpo", AHORA).titulo()).isEqualTo("Hola ");
    }

    @Test
    void reportaTodosLosCamposInvalidosConSusNombresJson() {
        assertThat(motivos(" ", "b".repeat(2001))).containsExactly(
                Map.entry("titulo", MotivoInvalido.REQUERIDO), Map.entry("cuerpo", MotivoInvalido.LONGITUD_MAXIMA));
    }

    @Test
    void elConstructorExigeUnIdValidoYTextosYaLimpios() {
        assertThatThrownBy(() -> new PreguntaFrecuente("no-es-un-id", "Título", "Cuerpo", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PreguntaFrecuente(ID, " Título", "Cuerpo", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PreguntaFrecuente(ID, "Título", "", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PreguntaFrecuente(ID, "Título", "Cuerpo", null))
                .isInstanceOf(NullPointerException.class);
    }
}
