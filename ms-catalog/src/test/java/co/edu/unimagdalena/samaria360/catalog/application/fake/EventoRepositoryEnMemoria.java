package co.edu.unimagdalena.samaria360.catalog.application.fake;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.EventoRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EventoTuristico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Colección {@code eventos} en memoria, con los mismos órdenes que {@code EventoMongoAdapter}. */
public class EventoRepositoryEnMemoria implements EventoRepositoryPort {

    private static final Comparator<EventoTuristico> RECIENTES_PRIMERO =
            Comparator.comparing(EventoTuristico::fechaRegistro).thenComparing(EventoTuristico::id).reversed();
    private static final Comparator<EventoTuristico> PROXIMOS_PRIMERO =
            Comparator.comparing(EventoTuristico::fechaInicio).thenComparing(EventoTuristico::id);

    private final Map<String, EventoTuristico> eventos = new LinkedHashMap<>();
    private long secuencia;

    public void agregar(EventoTuristico evento) {
        eventos.put(evento.id(), evento);
    }

    public List<EventoTuristico> guardados() {
        return List.copyOf(eventos.values());
    }

    @Override
    public String nuevoId() {
        return String.format("%024x", ++secuencia);
    }

    @Override
    public void insertar(EventoTuristico evento) {
        eventos.put(evento.id(), evento);
    }

    @Override
    public PaginaResultados<EventoTuristico> listar(int pagina, int tamano) {
        return pagina(evento -> true, RECIENTES_PRIMERO, pagina, tamano);
    }

    @Override
    public PaginaResultados<EventoTuristico> vigentes(LocalDate desde, int pagina, int tamano) {
        return pagina(evento -> evento.vigenteEl(desde), PROXIMOS_PRIMERO, pagina, tamano);
    }

    private PaginaResultados<EventoTuristico> pagina(Predicate<EventoTuristico> filtro,
                                                     Comparator<EventoTuristico> orden, int pagina, int tamano) {
        List<EventoTuristico> todos = eventos.values().stream().filter(filtro).sorted(orden).toList();
        List<EventoTuristico> pedidos = todos.stream().skip((long) pagina * tamano).limit(tamano).toList();
        return new PaginaResultados<>(pedidos, pagina, tamano, todos.size());
    }
}
