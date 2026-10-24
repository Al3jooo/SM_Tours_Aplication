package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.model.BusquedaReciente;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CriteriosGuardados;

import java.util.List;
import java.util.UUID;

/**
 * CU «Consultar historial de búsquedas» (RF60, DEC-56, DEC-88): una llamada aparte de la búsqueda pública, que no
 * cambia. Solo del rol Turista, siempre el del claim {@code sub}.
 */
public interface HistorialBusquedasUseCase {

    /**
     * Guarda la búsqueda; la misma combinación de criterios solo actualiza su fecha y sube al primer lugar. Se conservan
     * las 10 más recientes. Una búsqueda sin criterios no se guarda.
     */
    void registrar(UUID turistaId, CriteriosGuardados criterios);

    /** Como máximo 10, la más reciente primero. */
    List<BusquedaReciente> listar(UUID turistaId);
}
