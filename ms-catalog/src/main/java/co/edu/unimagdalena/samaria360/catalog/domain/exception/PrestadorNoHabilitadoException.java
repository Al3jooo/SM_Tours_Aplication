package co.edu.unimagdalena.samaria360.catalog.domain.exception;

/**
 * El prestador no puede registrar servicios (RF09, DEC-125, DEC-139): su cuenta no está en la proyección {@code cuentas},
 * no está Activa o la proyección todavía no tiene su RNT. Es un 403 PRESTADOR_NO_HABILITADO.
 */
public class PrestadorNoHabilitadoException extends RuntimeException {

    public PrestadorNoHabilitadoException() {
        super("La cuenta del prestador no está habilitada para registrar servicios");
    }
}
