package co.edu.unimagdalena.samaria360.auth.domain.exception;

/** Prestador registrado que el Administrador aún no valida (RF02, RF28; decisión D3). */
public class CuentaPendienteValidacionException extends InicioSesionException {

    public CuentaPendienteValidacionException() {
        super("La cuenta está pendiente de validación por el Administrador");
    }
}
