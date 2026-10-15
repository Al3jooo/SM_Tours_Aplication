package co.edu.unimagdalena.samaria360.booking.domain.exception;

/**
 * La reserva está Completada (409 {@code RESERVA_NO_CANCELABLE}).
 */
public class ReservaNoCancelableException extends RuntimeException {

    public ReservaNoCancelableException() {
        super("La reserva ya se completó y no se puede cancelar");
    }
}
