package co.edu.unimagdalena.samaria360.booking.domain.exception;

/**
 * La reserva no existe o es de otro turista: no se revela cuál (404 {@code RESERVA_NO_ENCONTRADA}).
 */
public class ReservaNoEncontradaException extends RuntimeException {

    public ReservaNoEncontradaException() {
        super("La reserva no existe");
    }
}
