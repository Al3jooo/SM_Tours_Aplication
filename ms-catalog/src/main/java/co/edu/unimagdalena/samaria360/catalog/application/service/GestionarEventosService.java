package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.GestionarEventosUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.EventoRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorEvento;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EventoTuristico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** CU «Registrar evento en agenda» (RF25). El Gestor es el del token; la fecha de registro, la de {@link RelojPort}. */
public class GestionarEventosService implements GestionarEventosUseCase {

    private final EventoRepositoryPort eventos;
    private final RelojPort reloj;

    public GestionarEventosService(EventoRepositoryPort eventos, RelojPort reloj) {
        this.eventos = Objects.requireNonNull(eventos);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /** La fecha va en milisegundos, como la guarda MongoDB. */
    @Override
    public EventoTuristico registrar(UUID gestorId, BorradorEvento borrador) {
        EventoTuristico evento = EventoTuristico.registrar(eventos.nuevoId(), gestorId, borrador,
                reloj.ahora().truncatedTo(ChronoUnit.MILLIS));
        eventos.insertar(evento);
        return evento;
    }

    @Override
    public PaginaResultados<EventoTuristico> listar(int pagina) {
        if (pagina < 0) {
            throw new ContenidoInvalidoException(Map.of("pagina", MotivoInvalido.FORMATO));
        }
        return eventos.listar(pagina, TAMANO_PAGINA);
    }
}
