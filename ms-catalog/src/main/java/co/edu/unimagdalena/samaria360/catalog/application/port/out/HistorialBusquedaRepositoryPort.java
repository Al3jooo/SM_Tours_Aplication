package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import co.edu.unimagdalena.samaria360.catalog.domain.model.BusquedaReciente;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CriteriosGuardados;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Colección {@code historial_busqueda} de catalog_db (M007). */
public interface HistorialBusquedaRepositoryPort {

    /**
     * Upsert por {@code (turistaId, clave)}: crea el registro o actualiza sus criterios y su fecha.
     *
     * @return false si el índice único lo rechazó (otro registro de la misma combinación llegó en el mismo instante)
     */
    boolean guardar(UUID turistaId, CriteriosGuardados criterios, Instant fecha);

    /**
     * Solo actualiza el registro existente de la misma combinación.
     *
     * @return false si no se actualizó nada
     */
    boolean actualizar(UUID turistaId, CriteriosGuardados criterios, Instant fecha);

    /** Borra las que pasen de {@code conservar}, empezando por la más antigua. */
    void podar(UUID turistaId, int conservar);

    /** La más reciente primero. */
    List<BusquedaReciente> listar(UUID turistaId, int maximo);
}
