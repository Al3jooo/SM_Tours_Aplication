package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import java.util.UUID;

/** ¿Sigue vigente la sesión de este token frente a la versión conocida de la cuenta? (DEC-29). */
public interface VerificarSesionUseCase {

    boolean sesionVigente(UUID idUsuario, long ver);
}
