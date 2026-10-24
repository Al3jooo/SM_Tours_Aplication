package co.edu.unimagdalena.samaria360.auth.domain.exception;

/** Un Administrador no puede cambiar el rol de su propia cuenta (409 CAMBIO_ROL_PROPIO, DEC-82). */
public class CambioRolPropioException extends RuntimeException {

    public CambioRolPropioException() {
        super("Un Administrador no puede cambiar el rol de su propia cuenta");
    }
}
