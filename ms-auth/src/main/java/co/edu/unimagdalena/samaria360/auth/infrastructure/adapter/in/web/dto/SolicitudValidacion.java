package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto;

/**
 * Decisión sobre un prestador Pendiente (RF28): {@code decision} es «Aprobar» o «Rechazar»; la justificación solo se
 * exige al rechazar. La validación la hace el caso de uso, que responde un código por campo.
 */
public record SolicitudValidacion(String decision, String justificacion) {
}
