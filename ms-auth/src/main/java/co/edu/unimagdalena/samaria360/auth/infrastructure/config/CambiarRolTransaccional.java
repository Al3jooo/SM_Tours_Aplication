package co.edu.unimagdalena.samaria360.auth.infrastructure.config;

import co.edu.unimagdalena.samaria360.auth.application.port.in.CambiarRolUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoCambioRol;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PersonalResumen;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Todo el cambio de rol en una transacción: desde la primera lectura hasta el evento del outbox, pasando por el bloqueo
 * de los Administradores activos (DEC-106). Se confirma todo o nada (DEC-26): cualquier excepción —400, 404, 409 o un
 * fallo al escribir el evento— revierte el rol, las filas de los subtipos y la versión.
 */
public class CambiarRolTransaccional implements CambiarRolUseCase {

    private final CambiarRolUseCase delegado;
    private final TransactionTemplate transaccion;

    public CambiarRolTransaccional(CambiarRolUseCase delegado, TransactionTemplate transaccion) {
        this.delegado = delegado;
        this.transaccion = transaccion;
    }

    @Override
    public PersonalResumen cambiarRol(ComandoCambioRol comando) {
        return transaccion.execute(estado -> delegado.cambiarRol(comando));
    }
}
