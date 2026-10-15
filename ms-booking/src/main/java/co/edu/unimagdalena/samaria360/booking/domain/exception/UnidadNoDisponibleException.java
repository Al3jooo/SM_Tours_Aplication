package co.edu.unimagdalena.samaria360.booking.domain.exception;

/**
 * El alojamiento existe y se puede reservar, pero no tiene esa unidad (D1, 409 {@code UNIDAD_NO_DISPONIBLE}): el
 * cliente debe volver a cargar las unidades del detalle.
 */
public class UnidadNoDisponibleException extends RuntimeException {

    public UnidadNoDisponibleException(String idUnidad) {
        super("El alojamiento no tiene la unidad " + idUnidad);
    }
}
