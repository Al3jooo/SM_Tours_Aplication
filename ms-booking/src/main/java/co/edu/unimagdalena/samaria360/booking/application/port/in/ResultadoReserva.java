package co.edu.unimagdalena.samaria360.booking.application.port.in;

import co.edu.unimagdalena.samaria360.booking.domain.model.Reserva;

/** @param repetida true si la clave ya se había usado con el mismo pedido: es la reserva de entonces (D6) */
public record ResultadoReserva(Reserva reserva, boolean repetida) {
}
