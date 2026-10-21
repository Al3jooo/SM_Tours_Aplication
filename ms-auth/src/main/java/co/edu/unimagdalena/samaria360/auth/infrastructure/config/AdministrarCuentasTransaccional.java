package co.edu.unimagdalena.samaria360.auth.infrastructure.config;

import co.edu.unimagdalena.samaria360.auth.application.port.in.AdministrarCuentasUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoValidacion;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PrestadorDetalle;
import co.edu.unimagdalena.samaria360.auth.application.port.in.TuristaResumen;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/**
 * Cada cambio de estado en una transacción: el UPDATE condicional y el evento del outbox se confirman juntos o no se
 * confirma nada (DEC-26). Cualquier excepción —409, 404, 400 o un fallo al escribir el evento— revierte todo.
 */
public class AdministrarCuentasTransaccional implements AdministrarCuentasUseCase {

    private final AdministrarCuentasUseCase delegado;
    private final TransactionTemplate transaccion;

    public AdministrarCuentasTransaccional(AdministrarCuentasUseCase delegado, TransactionTemplate transaccion) {
        this.delegado = delegado;
        this.transaccion = transaccion;
    }

    @Override
    public PrestadorDetalle validar(ComandoValidacion comando) {
        return transaccion.execute(estado -> delegado.validar(comando));
    }

    @Override
    public PrestadorDetalle inactivar(UUID idPrestador) {
        return transaccion.execute(estado -> delegado.inactivar(idPrestador));
    }

    @Override
    public TuristaResumen desactivar(UUID idTurista) {
        return transaccion.execute(estado -> delegado.desactivar(idTurista));
    }

    @Override
    public TuristaResumen reactivar(UUID idTurista) {
        return transaccion.execute(estado -> delegado.reactivar(idTurista));
    }
}
