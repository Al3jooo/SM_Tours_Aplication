package co.edu.unimagdalena.samaria360.auth.application.port.in;

import java.util.UUID;

/** Decisión del Administrador sobre un prestador Pendiente (RF28): «Aprobar» o «Rechazar» con justificación. */
public record ComandoValidacion(UUID idPrestador, String decision, String justificacion, UUID idAdministrador) {
}
