package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Una búsqueda del historial del turista (RF60): sus criterios y la última vez que la hizo.
 *
 * @param id _id del documento en {@code historial_busqueda}, como ObjectId en texto
 */
public record BusquedaReciente(String id, CriteriosGuardados criterios, Instant fecha) {

    /** RF60: «historial de sus últimas 10 búsquedas realizadas». */
    public static final int MAXIMO_POR_TURISTA = 10;

    public BusquedaReciente {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(criterios, "criterios");
        Objects.requireNonNull(fecha, "fecha");
    }
}
