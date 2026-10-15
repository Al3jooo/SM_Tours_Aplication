package co.edu.unimagdalena.samaria360.booking.domain.exception;

/**
 * El servicio no existe en el catálogo, no está Publicado, es de un prestador inactivo o no tiene precio: no se
 * puede reservar (409 {@code OFERTA_NO_DISPONIBLE}).
 */
public class OfertaNoDisponibleException extends RuntimeException {

    public OfertaNoDisponibleException(String idServicio) {
        super("El servicio " + idServicio + " no está disponible para reservar");
    }
}
