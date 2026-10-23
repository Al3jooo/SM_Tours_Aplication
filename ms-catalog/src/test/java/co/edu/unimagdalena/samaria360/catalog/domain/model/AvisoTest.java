package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Aviso general (RF40, DEC-58): mensaje de 1 a 300 caracteres sin los extremos y tipo Clima, Cierre o Comunicado. */
class AvisoTest {

    private static final String ID = "6a1e00000000000000000001";
    private static final Instant AHORA = Instant.parse("2026-10-22T15:00:00Z");

    private static Map<String, MotivoInvalido> motivos(String mensaje, String tipo) {
        try {
            Aviso.publicar(ID, mensaje, tipo, AHORA);
        } catch (ContenidoInvalidoException e) {
            return e.getCampos();
        }
        return Map.of();
    }

    @ParameterizedTest
    @EnumSource(TipoAviso.class)
    void seCreaActivoConElMensajeLimpioYCualquierTipoDeRF40(TipoAviso tipo) {
        Aviso aviso = Aviso.publicar(ID, "  Lluvias fuertes en la Sierra  ", tipo.name(), AHORA);

        assertThat(aviso.activo()).isTrue();
        assertThat(aviso.tipo()).isEqualTo(tipo);
        assertThat(aviso.mensaje()).isEqualTo("Lluvias fuertes en la Sierra");
        assertThat(aviso.fechaPublicacion()).isEqualTo(AHORA);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "    ", "\n"})
    void unMensajeVacioOSoloConEspaciosEsRequerido(String vacio) {
        assertThat(motivos(vacio, "Clima")).containsExactly(Map.entry("mensaje", MotivoInvalido.REQUERIDO));
    }

    @Test
    void elMensajeAdmiteHasta300CaracteresYNoUnoMas() {
        assertThat(motivos("m".repeat(300), "Comunicado")).isEmpty();
        assertThat(motivos(" " + "m".repeat(300) + " ", "Comunicado")).isEmpty();
        assertThat(motivos("m".repeat(301), "Comunicado"))
                .containsExactly(Map.entry("mensaje", MotivoInvalido.LONGITUD_MAXIMA));
        assertThat(motivos(PreguntaFrecuenteTest.EMOJI.repeat(300), "Comunicado")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Lluvia", "clima", "CIERRE", " Cierre"})
    void unTipoFueraDelVocabularioNoEstaPermitido(String tipo) {
        assertThat(motivos("Mensaje", tipo)).containsExactly(Map.entry("tipo", MotivoInvalido.NO_PERMITIDO));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void sinTipoEsRequerido(String tipo) {
        assertThat(motivos("Mensaje", tipo)).containsExactly(Map.entry("tipo", MotivoInvalido.REQUERIDO));
    }

    @Test
    void reportaLosDosCamposConSusNombresJson() {
        assertThat(motivos("", "Lluvia")).containsExactly(
                Map.entry("mensaje", MotivoInvalido.REQUERIDO), Map.entry("tipo", MotivoInvalido.NO_PERMITIDO));
    }

    @Test
    void elConstructorExigeUnIdValidoYUnMensajeYaLimpio() {
        assertThatThrownBy(() -> new Aviso("123", "Mensaje", TipoAviso.Clima, true, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Aviso(ID, "Mensaje ", TipoAviso.Clima, true, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Aviso(ID, "Mensaje", null, true, AHORA))
                .isInstanceOf(NullPointerException.class);
    }
}
