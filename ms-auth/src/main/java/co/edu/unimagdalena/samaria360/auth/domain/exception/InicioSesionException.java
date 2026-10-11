package co.edu.unimagdalena.samaria360.auth.domain.exception;

/** Rechazo del inicio de sesión (RF15). */
public abstract class InicioSesionException extends RuntimeException {

    protected InicioSesionException(String mensaje) {
        super(mensaje);
    }
}
