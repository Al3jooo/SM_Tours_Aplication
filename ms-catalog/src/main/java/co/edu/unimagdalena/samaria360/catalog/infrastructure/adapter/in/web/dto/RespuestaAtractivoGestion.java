package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;

import java.time.Instant;
import java.util.List;

/**
 * Un atractivo para el Gestor (DEC-206): los datos de {@link RespuestaAtractivo} más quién lo registró, las fechas y la
 * versión, que el Gestor envía al editar o al cambiar el estado (DEC-207).
 *
 * @param gestorId           el Gestor que lo registró (no restringe quién lo edita)
 * @param fechaRegistro      null solo en un atractivo anterior a M012 que no la recibió
 * @param fechaActualizacion la de la última edición o cambio de estado; null si nunca se editó
 */
public record RespuestaAtractivoGestion(String id, String nombre, String categoria, String descripcion,
                                        RespuestaAtractivo.Coordenadas coordenadas, List<String> imagenes,
                                        int limiteCapacidadCarga, String estadoOperativo, String normasAmbientales,
                                        String horarioAtencion, List<String> condicionesAccesibilidad,
                                        String gestorId, Instant fechaRegistro, Instant fechaActualizacion,
                                        int version) {

    public static RespuestaAtractivoGestion de(AtractivoPublico atractivo) {
        return new RespuestaAtractivoGestion(atractivo.getId().orElseThrow(), atractivo.getNombre(),
                atractivo.getCategoria().orElse(null), atractivo.getDescripcion().orElse(null),
                RespuestaAtractivo.coordenadas(atractivo), atractivo.getImagenes(),
                atractivo.getLimiteCapacidadCarga(), atractivo.getEstadoOperativo().map(Enum::name).orElse(null),
                atractivo.getNormasAmbientales().orElse(null), atractivo.getHorarioAtencion().orElse(null),
                RespuestaAtractivo.accesibilidad(atractivo), atractivo.getGestorId().toString(),
                atractivo.getFechaRegistro().orElse(null), atractivo.getFechaActualizacion().orElse(null),
                atractivo.getVersion());
    }
}
