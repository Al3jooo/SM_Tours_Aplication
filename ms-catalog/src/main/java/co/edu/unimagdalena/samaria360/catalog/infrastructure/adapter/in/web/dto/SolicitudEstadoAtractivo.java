package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

/**
 * Cuerpo de {@code POST /api/v1/catalogo/gestor/atractivos/{id}/estado} (RF33, DEC-210).
 *
 * @param estadoNuevo {@code Abierto}, {@code Cerrado_temporalmente} o {@code En_mantenimiento}, distinto del actual
 * @param version     la versión del atractivo que el Gestor leyó (DEC-207)
 */
public record SolicitudEstadoAtractivo(String estadoNuevo, Integer version) {
}
