package co.edu.unimagdalena.samaria360.booking.application.port.out;

/**
 * Otra reserva del mismo turista ya usa esa {@code Idempotency-Key} (restricción única de la base, D6). Llega dentro
 * de la transacción, que se revierte; después se lee la reserva ganadora.
 */
public class ClaveIdempotenciaRepetidaException extends RuntimeException {

    public ClaveIdempotenciaRepetidaException(Throwable causa) {
        super("La clave de idempotencia ya existe para este turista", causa);
    }
}
