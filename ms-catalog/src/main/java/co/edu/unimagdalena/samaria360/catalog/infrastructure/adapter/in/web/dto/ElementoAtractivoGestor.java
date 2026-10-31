package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;

import java.time.Instant;

/**
 * Una fila del panel «Atractivos» del Gestor (WF-24, DEC-206).
 *
 * @param imagen la primera imagen (la portada); null si no tiene
 */
public record ElementoAtractivoGestor(String id, String nombre, String categoria, String estadoOperativo,
                                      int limiteCapacidadCarga, String imagen, Instant fechaRegistro, int version) {

    public static ElementoAtractivoGestor de(AtractivoPublico atractivo) {
        return new ElementoAtractivoGestor(atractivo.getId().orElseThrow(), atractivo.getNombre(),
                atractivo.getCategoria().orElse(null), atractivo.getEstadoOperativo().map(Enum::name).orElse(null),
                atractivo.getLimiteCapacidadCarga(), atractivo.getImagenes().stream().findFirst().orElse(null),
                atractivo.getFechaRegistro().orElse(null), atractivo.getVersion());
    }
}
