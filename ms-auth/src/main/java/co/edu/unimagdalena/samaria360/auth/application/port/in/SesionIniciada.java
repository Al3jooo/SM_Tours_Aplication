package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;

import java.time.Instant;
import java.util.UUID;

/** Resultado de un inicio de sesión exitoso. */
public record SesionIniciada(String token, Instant expiraEn, UUID idUsuario, Rol rol) {
}
