package co.edu.unimagdalena.samaria360.auth.application.port.in;

import java.util.UUID;

/** ¿Sigue vigente la sesión de este token frente a la versión de la cuenta? (DEC-29). */
public interface VerificarSesionUseCase {

    boolean sesionVigente(UUID idUsuario, long ver);
}
