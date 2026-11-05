package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorEvento;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EventoTuristico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

import java.util.UUID;

/**
 * CU «Registrar evento en agenda» (RF25) y su flujo «Consultar eventos registrados». Solo el Gestor (la autorización la
 * aplica la infraestructura). Sin edición ni borrado (DEC-215).
 */
public interface GestionarEventosUseCase {

    int TAMANO_PAGINA = 10;

    /** Un evento nuevo, publicado de inmediato en la agenda (RF25: «publicándolos automáticamente»). */
    EventoTuristico registrar(UUID gestorId, BorradorEvento borrador);

    /** Todos los eventos, también los vencidos, del más reciente al más antiguo, en páginas de 10 desde 0. */
    PaginaResultados<EventoTuristico> listar(int pagina);
}
