package co.edu.unimagdalena.samaria360.booking.domain.exception;

/**
 * Falta la fila de inventario de alguna noche o del horario pedido (409 {@code FECHA_NO_DISPONIBLE}): esa fecha
 * no se ofrece.
 */
public class FechaNoDisponibleException extends RuntimeException {

    public FechaNoDisponibleException() {
        super("No hay disponibilidad publicada para alguna de las fechas pedidas");
    }
}
