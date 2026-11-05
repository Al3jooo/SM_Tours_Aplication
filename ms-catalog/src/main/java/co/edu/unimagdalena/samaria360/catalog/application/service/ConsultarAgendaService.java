package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarAgendaUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.EventoRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EventoTuristico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

import java.util.Map;
import java.util.Objects;

/** CU «Consultar agenda de eventos» (RF25): «hoy» es el día de {@link RelojPort} en Bogotá (DEC-215). */
public class ConsultarAgendaService implements ConsultarAgendaUseCase {

    private final EventoRepositoryPort eventos;
    private final RelojPort reloj;

    public ConsultarAgendaService(EventoRepositoryPort eventos, RelojPort reloj) {
        this.eventos = Objects.requireNonNull(eventos);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public PaginaResultados<EventoTuristico> agenda(int pagina) {
        if (pagina < 0) {
            throw new ContenidoInvalidoException(Map.of("pagina", MotivoInvalido.FORMATO));
        }
        return eventos.vigentes(EventoTuristico.hoy(reloj.ahora()), pagina, TAMANO_PAGINA);
    }
}
