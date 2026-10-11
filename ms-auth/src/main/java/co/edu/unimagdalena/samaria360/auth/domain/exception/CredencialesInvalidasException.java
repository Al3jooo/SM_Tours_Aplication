package co.edu.unimagdalena.samaria360.auth.domain.exception;

/**
 * Correo inexistente o contraseña incorrecta. Usa el mismo mensaje en ambos casos para no
 * revelar qué correos están registrados.
 */
public class CredencialesInvalidasException extends InicioSesionException {

    public static final String MENSAJE = "Correo o contraseña incorrectos";

    public CredencialesInvalidasException() {
        super(MENSAJE);
    }
}
