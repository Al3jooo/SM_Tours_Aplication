package co.edu.unimagdalena.samaria360.auth.domain.exception;

import java.time.Instant;

/** Cuenta bloqueada temporalmente por intentos fallidos (RF15). */
public class CuentaBloqueadaException extends InicioSesionException {

    private final Instant bloqueadoHasta;

    public CuentaBloqueadaException(Instant bloqueadoHasta) {
        super("Cuenta bloqueada temporalmente por intentos fallidos");
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
