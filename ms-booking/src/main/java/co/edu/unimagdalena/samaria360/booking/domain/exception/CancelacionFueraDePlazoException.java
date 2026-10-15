package co.edu.unimagdalena.samaria360.booking.domain.exception;

/**
 * Faltan menos de 24 horas para el inicio (RF44, 409 {@code CANCELACION_FUERA_DE_PLAZO}).
 */
public class CancelacionFueraDePlazoException extends RuntimeException {

    public CancelacionFueraDePlazoException() {
        super("El tiempo límite para cancelar expiró: debe hacerse al menos 24 horas antes del inicio");
    }
}
