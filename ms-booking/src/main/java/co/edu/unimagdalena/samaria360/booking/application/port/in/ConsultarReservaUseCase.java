package co.edu.unimagdalena.samaria360.booking.application.port.in;

import co.edu.unimagdalena.samaria360.booking.domain.model.Reserva;

import java.util.UUID;

/** Una reserva propia; la de otro turista no existe (404). Nunca consulta el catálogo: responde lo guardado (RF32). */
public interface ConsultarReservaUseCase {

    Reserva consultar(UUID idReserva, UUID idTurista);
}
