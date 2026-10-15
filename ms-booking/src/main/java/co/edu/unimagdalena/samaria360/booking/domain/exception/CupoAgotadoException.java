package co.edu.unimagdalena.samaria360.booking.domain.exception;

/**
 * Hay inventario, pero no alcanza en alguna noche u horario (RF11, 409 {@code CUPO_AGOTADO}). No se descuenta
 * nada.
 */
public class CupoAgotadoException extends RuntimeException {

    public CupoAgotadoException() {
        super("No quedan cupos suficientes para las fechas pedidas");
    }
}
