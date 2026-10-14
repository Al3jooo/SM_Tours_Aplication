package co.edu.unimagdalena.samaria360.auth.domain.exception;

/**
 * Otro registro con el mismo correo terminó primero (restricción única de la base). Nunca llega al cliente: el registro
 * se reintenta y sigue el camino del correo existente, con la misma respuesta (ADR-006).
 */
public class CorreoYaRegistradoException extends RuntimeException {

    public CorreoYaRegistradoException() {
        super("El correo ya está registrado");
    }
}
