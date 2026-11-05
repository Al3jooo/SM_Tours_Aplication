package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.fake.EventoRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EventoTuristico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.MotivoInvalido;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CU «Consultar agenda de eventos» (RF25): vigentes en Bogotá y por fecha de inicio (DEC-215). */
class ConsultarAgendaServiceTest {

    private static final UUID GESTOR = UUID.randomUUID();
    private static final Instant REGISTRO = Instant.parse("2026-10-01T15:00:00Z");

    private final EventoRepositoryEnMemoria eventos = new EventoRepositoryEnMemoria();
    private final RelojFijo reloj = new RelojFijo(REGISTRO);
    private final ConsultarAgendaService servicio = new ConsultarAgendaService(eventos, reloj);

    private void guardar(String id, String nombre, String inicio, String fin) {
        eventos.agregar(new EventoTuristico(id, GESTOR, nombre, LocalDate.parse(inicio), LocalDate.parse(fin),
                "Lugar", "Descripción", REGISTRO));
    }

    /** El evento termina el 5: a las 23:59 del 5 en Bogotá (04:59Z del 6) sigue; a las 00:00 del 6, no. */
    @Test
    void unEventoSaleDeLaAgendaALaMedianocheDeBogotaDespuesDeSuUltimoDia() {
        guardar("0000000000000000000000e1", "Termina el 5", "2026-11-03", "2026-11-05");

        reloj.ajustar(Instant.parse("2026-11-06T04:59:59Z"));
        assertThat(servicio.agenda(0).elementos()).extracting(EventoTuristico::nombre).containsExactly("Termina el 5");

        reloj.ajustar(Instant.parse("2026-11-06T05:00:00Z"));
        assertThat(servicio.agenda(0).elementos()).isEmpty();
    }

    @Test
    void ordenaPorFechaDeInicioAscendenteConElIdDeDesempate() {
        reloj.ajustar(Instant.parse("2026-11-05T15:00:00Z"));
        guardar("0000000000000000000000e3", "Diciembre", "2026-12-01", "2026-12-02");
        guardar("0000000000000000000000e2", "En curso", "2026-11-01", "2026-11-30");
        guardar("0000000000000000000000e5", "Noviembre B", "2026-11-20", "2026-11-20");
        guardar("0000000000000000000000e4", "Noviembre A", "2026-11-20", "2026-11-21");
        guardar("0000000000000000000000e1", "Vencido", "2026-10-01", "2026-11-04");

        assertThat(servicio.agenda(0).elementos()).extracting(EventoTuristico::nombre)
                .containsExactly("En curso", "Noviembre A", "Noviembre B", "Diciembre");
        assertThat(servicio.agenda(0).totalElementos()).isEqualTo(4);
    }

    @Test
    void unaPaginaNegativaEsFormato() {
        assertThatThrownBy(() -> servicio.agenda(-1))
                .isInstanceOfSatisfying(ContenidoInvalidoException.class, e -> assertThat(e.getCampos())
                        .containsExactly(Map.entry("pagina", MotivoInvalido.FORMATO)));
    }
}
