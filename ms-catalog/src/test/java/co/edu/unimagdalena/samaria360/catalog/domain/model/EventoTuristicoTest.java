package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RF25: las reglas del registro de un evento (DEC-215, DEC-235 a DEC-237 y DEC-242). */
class EventoTuristicoTest {

    private static final String ID = "0000000000000000000000e1";
    private static final UUID GESTOR = UUID.fromString("00000000-0000-4000-8000-000000000003");
    /** 2026-11-05 a las 10:00 en Bogotá. */
    private static final Instant AHORA = Instant.parse("2026-11-05T15:00:00Z");
    /** Fuera del plano básico: un punto de código, dos unidades UTF-16. */
    private static final String EMOJI = "🌴";

    private static BorradorEvento borrador(String inicio, String fin) {
        return new BorradorEvento("  Festival del Mar  ", inicio, fin, " Bahía de Santa Marta ",
                " Música y gastronomía. ");
    }

    @Test
    void registraUnEventoConLosTextosLimpiosYLasFechasDeDia() {
        EventoTuristico evento = EventoTuristico.registrar(ID, GESTOR, borrador("2026-12-01", "2026-12-03"), AHORA);

        assertThat(evento.nombre()).isEqualTo("Festival del Mar");
        assertThat(evento.ubicacion()).isEqualTo("Bahía de Santa Marta");
        assertThat(evento.descripcion()).isEqualTo("Música y gastronomía.");
        assertThat(evento.fechaInicio()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(evento.fechaFin()).isEqualTo(LocalDate.of(2026, 12, 3));
        assertThat(evento.gestorId()).isEqualTo(GESTOR);
        assertThat(evento.fechaRegistro()).isEqualTo(AHORA);
    }

    @Test
    void unEventoDeUnSoloDiaQueTerminaHoyEsValido() {
        EventoTuristico evento = EventoTuristico.registrar(ID, GESTOR, borrador("2026-11-05", "2026-11-05"), AHORA);

        assertThat(evento.fechaInicio()).isEqualTo(evento.fechaFin());
    }

    @Test
    void sinDatosCadaCampoEsRequeridoEnElOrdenDelCuerpo() {
        assertThatThrownBy(() -> EventoTuristico.registrar(ID, GESTOR, null, AHORA))
                .isInstanceOfSatisfying(ContenidoInvalidoException.class, e -> assertThat(e.getCampos())
                        .containsExactly(Map.entry("nombre", MotivoInvalido.REQUERIDO),
                                Map.entry("fechaInicio", MotivoInvalido.REQUERIDO),
                                Map.entry("fechaFin", MotivoInvalido.REQUERIDO),
                                Map.entry("ubicacion", MotivoInvalido.REQUERIDO),
                                Map.entry("descripcion", MotivoInvalido.REQUERIDO)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-02-30", "01/12/2026", "2026-12-1", "+2026-12-01", "2026-13-01", "mañana"})
    void unaFechaQueNoEsUnDiaIsoEsFormato(String fecha) {
        assertThat(campos(borrador(fecha, fecha))).containsEntry("fechaInicio", MotivoInvalido.FORMATO)
                .containsEntry("fechaFin", MotivoInvalido.FORMATO);
    }

    /** DEC-237: el último día no puede ser anterior al primero. */
    @Test
    void unaFechaFinAnteriorALaDeInicioNoEsPermitida() {
        assertThat(campos(borrador("2026-12-03", "2026-12-01")))
                .containsExactly(Map.entry("fechaFin", MotivoInvalido.NO_PERMITIDO));
    }

    /** DEC-242 (decisión del equipo, no del SRS): un evento ya terminado en Bogotá no se registra. */
    @Test
    void unaFechaFinAnteriorAHoyEnBogotaNoEsPermitida() {
        assertThat(campos(borrador("2026-11-01", "2026-11-04")))
                .containsExactly(Map.entry("fechaFin", MotivoInvalido.NO_PERMITIDO));
    }

    /** A las 23:30 del 5 en Bogotá ya es el 6 en UTC: «hoy» sigue siendo el 5. */
    @Test
    void hoyEsElDiaDeBogotaNoElDeUtc() {
        Instant nocheEnBogota = Instant.parse("2026-11-06T04:30:00Z");

        assertThat(EventoTuristico.hoy(nocheEnBogota)).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(EventoTuristico.registrar(ID, GESTOR, borrador("2026-11-05", "2026-11-05"), nocheEnBogota)
                .fechaFin()).isEqualTo(LocalDate.of(2026, 11, 5));
    }

    @Test
    void losLimitesSeCuentanEnPuntosDeCodigoDespuesDeRecortar() {
        BorradorEvento enElLimite = new BorradorEvento(" " + EMOJI.repeat(120) + " ", "2026-12-01", "2026-12-01",
                EMOJI.repeat(200), EMOJI.repeat(1000));
        BorradorEvento pasado = new BorradorEvento(EMOJI.repeat(121), "2026-12-01", "2026-12-01",
                EMOJI.repeat(201), EMOJI.repeat(1001));

        assertThat(EventoTuristico.registrar(ID, GESTOR, enElLimite, AHORA).nombre()).isEqualTo(EMOJI.repeat(120));
        assertThat(campos(pasado)).containsExactly(Map.entry("nombre", MotivoInvalido.LONGITUD_MAXIMA),
                Map.entry("ubicacion", MotivoInvalido.LONGITUD_MAXIMA),
                Map.entry("descripcion", MotivoInvalido.LONGITUD_MAXIMA));
    }

    @Test
    void soloEspaciosEsRequerido() {
        BorradorEvento espacios = new BorradorEvento("   ", " ", "\t", " ", "\n");

        assertThat(campos(espacios)).containsOnlyKeys("nombre", "fechaInicio", "fechaFin", "ubicacion", "descripcion")
                .allSatisfy((campo, motivo) -> assertThat(motivo).isEqualTo(MotivoInvalido.REQUERIDO));
    }

    @Test
    void vigenteHastaSuUltimoDiaIncluido() {
        EventoTuristico evento = EventoTuristico.registrar(ID, GESTOR, borrador("2026-12-01", "2026-12-03"), AHORA);

        assertThat(evento.vigenteEl(LocalDate.of(2026, 12, 3))).isTrue();
        assertThat(evento.vigenteEl(LocalDate.of(2026, 12, 4))).isFalse();
    }

    @Test
    void elConstructorRechazaUnEventoInconsistente() {
        LocalDate dia = LocalDate.of(2026, 12, 1);

        assertThatThrownBy(() -> new EventoTuristico(ID, GESTOR, "Evento", dia, dia.minusDays(1), "Lugar", "Texto",
                AHORA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EventoTuristico("abc", GESTOR, "Evento", dia, dia, "Lugar", "Texto", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EventoTuristico(ID, GESTOR, " Evento", dia, dia, "Lugar", "Texto", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Map<String, MotivoInvalido> campos(BorradorEvento borrador) {
        try {
            EventoTuristico.registrar(ID, GESTOR, borrador, AHORA);
        } catch (ContenidoInvalidoException e) {
            return e.getCampos();
        }
        throw new AssertionError("Se esperaba ContenidoInvalidoException");
    }
}
