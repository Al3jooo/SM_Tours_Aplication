package co.edu.unimagdalena.samaria360.booking.domain.exception;

/**
 * La misma {@code Idempotency-Key} llegó con otro comando (D6, 422 {@code IDEMPOTENCIA_CONFLICTO}).
 */
public class IdempotenciaConflictoException extends RuntimeException {

    public IdempotenciaConflictoException() {
        super("La clave de idempotencia ya se usó con otra solicitud");
    }
}
