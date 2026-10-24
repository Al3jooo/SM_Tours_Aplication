package co.edu.unimagdalena.samaria360.auth.domain.exception;

/** El cambio dejaría el sistema sin Administradores activos (409 ULTIMO_ADMINISTRADOR, DEC-82). */
public class UltimoAdministradorException extends RuntimeException {

    public UltimoAdministradorException() {
        super("Debe quedar al menos un Administrador activo");
    }
}
