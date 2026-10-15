package co.edu.unimagdalena.samaria360.booking.application.port.in;

import co.edu.unimagdalena.samaria360.booking.domain.model.Reserva;

import java.util.UUID;

/** Cancelar una reserva propia hasta 24 horas antes del inicio (RF18, RF44). Idempotente: cancelar dos veces no libera dos veces. */
public interface CancelarReservaUseCase {

    Reserva cancelar(UUID idReserva, UUID idTurista);
}
