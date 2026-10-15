package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.error;

/** Falta {@code Idempotency-Key} o no es un UUID (D6, 400 {@code IDEMPOTENCIA_REQUERIDA}). */
public class IdempotenciaRequeridaException extends RuntimeException {

    public IdempotenciaRequeridaException() {
        super("La cabecera Idempotency-Key es obligatoria y debe ser un UUID");
    }
}
