package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

/**
 * Cuerpo de {@code POST /admin/servicios/{id}/aprobacion} (RF21, DEC-128), como la validación de prestadores de RF28.
 *
 * @param decision      {@code Aprobar} o {@code Rechazar}
 * @param justificacion obligatoria al rechazar; al aprobar se ignora
 */
public record SolicitudAprobacion(String decision, String justificacion) {
}
