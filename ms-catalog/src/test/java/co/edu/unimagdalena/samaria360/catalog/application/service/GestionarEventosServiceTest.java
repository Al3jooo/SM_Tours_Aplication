package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.fake.EventoRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorEvento;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EventoTuristico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CU «Registrar evento en agenda» (RF25) y «Consultar eventos registrados», con dobles de los puertos. */
class GestionarEventosServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-05T15:00:00.123456Z");
    private static final UUID GESTOR = UUID.randomUUID();

    private final EventoRepositoryEnMemoria eventos = new EventoRepositoryEnMemoria();
    private final RelojFijo reloj = new RelojFijo(AHORA);
    private final GestionarEventosService servicio = new GestionarEventosService(eventos, reloj);

    private static BorradorEvento borrador(String nombre) {
        return new BorradorEvento(nombre, "2026-12-01", "2026-12-02", "Rodadero", "Descripción");
    }

    @Test
    void registraConElGestorDelTokenYLaHoraDelRelojEnMilisegundos() {
        EventoTuristico evento = servicio.registrar(GESTOR, borrador("Festival"));

        assertThat(evento.gestorId()).isEqualTo(GESTOR);
        assertThat(evento.fechaRegistro()).isEqualTo(Instant.parse("2026-11-05T15:00:00.123Z"));
        assertThat(eventos.guardados()).containsExactly(evento);
    }

    @Test
    void unEventoInvalidoNoSeGuarda() {
        assertThatThrownBy(() -> servicio.registrar(GESTOR, borrador(" ")))
                .isInstanceOfSatisfying(ContenidoInvalidoException.class, e -> assertThat(e.getCampos())
                        .containsExactly(Map.entry("nombre", MotivoInvalido.REQUERIDO)));
        assertThat(eventos.guardados()).isEmpty();
    }

    @Test
    void elPanelListaTodosDelMasRecienteAlMasAntiguoDeDiezEnDiez() {
        for (int i = 0; i < 12; i++) {
            servicio.registrar(GESTOR, borrador("Evento " + i));
            reloj.avanzar(Duration.ofMinutes(1));
        }

        PaginaResultados<EventoTuristico> primera = servicio.listar(0);
        PaginaResultados<EventoTuristico> segunda = servicio.listar(1);

        assertThat(primera.elementos()).hasSize(10).first().extracting(EventoTuristico::nombre).isEqualTo("Evento 11");
        assertThat(segunda.elementos()).extracting(EventoTuristico::nombre).containsExactly("Evento 1", "Evento 0");
        assertThat(primera.totalElementos()).isEqualTo(12);
    }

    @Test
    void unaPaginaNegativaEsFormato() {
        assertThatThrownBy(() -> servicio.listar(-1))
                .isInstanceOfSatisfying(ContenidoInvalidoException.class, e -> assertThat(e.getCampos())
                        .containsExactly(Map.entry("pagina", MotivoInvalido.FORMATO)));
    }
}
