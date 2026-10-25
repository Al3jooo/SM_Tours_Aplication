package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.Revision;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;

import java.time.Instant;

/**
 * Un servicio en «Mis servicios» (DEC-129, DEC-143): el resultado de la aprobación.
 *
 * @param motivoRechazo la justificación del Administrador (RF21); null si no está Rechazado
 */
public record ElementoServicioPrestador(String id, String nombreComercial, String categoria, String estadoPublicacion,
                                        String motivoRechazo, Instant fechaRegistro) {

    public static ElementoServicioPrestador de(ServicioComercial servicio) {
        return new ElementoServicioPrestador(servicio.getId().orElseThrow(), servicio.getNombreComercial(),
                servicio.getCategoria().name(), servicio.getEstadoPublicacion().name(),
                servicio.getRevision().map(Revision::motivoRechazo).orElse(null),
                servicio.getFechaRegistro().orElse(null));
    }
}
