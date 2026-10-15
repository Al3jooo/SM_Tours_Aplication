package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import co.edu.unimagdalena.samaria360.booking.application.port.in.CancelarReservaUseCase;
import co.edu.unimagdalena.samaria360.booking.domain.model.Reserva;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/** Cancelar en una transacción: el cambio de estado y la devolución de cupos se confirman juntos o no se confirma nada. */
public class CancelarReservaTransaccional implements CancelarReservaUseCase {

    private final CancelarReservaUseCase delegado;
    private final TransactionTemplate transaccion;

    public CancelarReservaTransaccional(CancelarReservaUseCase delegado, TransactionTemplate transaccion) {
        this.delegado = delegado;
        this.transaccion = transaccion;
    }

    @Override
    public Reserva cancelar(UUID idReserva, UUID idTurista) {
        return transaccion.execute(estado -> delegado.cancelar(idReserva, idTurista));
    }
}
