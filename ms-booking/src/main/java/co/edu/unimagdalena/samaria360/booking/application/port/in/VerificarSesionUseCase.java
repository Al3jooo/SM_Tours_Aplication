package co.edu.unimagdalena.samaria360.booking.application.port.in;

import java.util.UUID;

/**
 * ¿Sigue vigente la sesión de este token frente a la versión conocida de la cuenta? (DEC-29). El rol es el del claim
 * {@code rol}: un Gestor o un Administrador sin fila en la proyección no tiene sesión vigente (DEC-110).
 */
public interface VerificarSesionUseCase {

    boolean sesionVigente(UUID idUsuario, long ver, String rol);
}
