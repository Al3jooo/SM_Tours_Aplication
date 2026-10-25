package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;

import java.time.Instant;

/**
 * Un servicio en el listado del Administrador (RF21, DEC-144). El nombre del prestador vive en auth_db: aquí van su id y
 * el RNT copiado en el servicio.
 */
public record ElementoServicioAdmin(String id, String nombreComercial, String categoria, String idPrestador,
                                    String rnt, String estadoPublicacion, boolean prestadorActivo,
                                    Instant fechaRegistro) {

    public static ElementoServicioAdmin de(ServicioComercial servicio) {
        return new ElementoServicioAdmin(servicio.getId().orElseThrow(), servicio.getNombreComercial(),
                servicio.getCategoria().name(), servicio.getPrestadorId().toString(), servicio.getRnt().orElse(null),
                servicio.getEstadoPublicacion().name(), servicio.isPrestadorActivo(),
                servicio.getFechaRegistro().orElse(null));
    }
}
