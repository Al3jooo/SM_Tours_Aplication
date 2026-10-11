package co.edu.unimagdalena.samaria360.auth.domain.exception;

import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoCuenta;

/** La cuenta no está Activa y no puede iniciar sesión (RF22, RF62). */
public class CuentaInactivaException extends InicioSesionException {

    private final EstadoCuenta estadoCuenta;

    public CuentaInactivaException(EstadoCuenta estadoCuenta) {
        super("La cuenta no está activa");
        this.estadoCuenta = estadoCuenta;
    }

    public EstadoCuenta getEstadoCuenta() {
        return estadoCuenta;
    }
}
