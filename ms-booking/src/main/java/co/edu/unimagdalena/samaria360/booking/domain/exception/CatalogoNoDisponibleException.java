package co.edu.unimagdalena.samaria360.booking.domain.exception;

/**
 * ms-catalog no respondió (caído, lento o con el circuito abierto). Reservar responde 503
 * {@code SERVICIO_NO_DISPONIBLE} con {@code servicio: ms-catalog}, el mismo significado que en el gateway.
 */
public class CatalogoNoDisponibleException extends RuntimeException {

    public CatalogoNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
