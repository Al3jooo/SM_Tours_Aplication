package co.edu.unimagdalena.samaria360.catalog.domain.exception;

/**
 * El servicio no existe, no está Publicado o es de un prestador inactivo (RF16, RF22). No se distingue cuál de los
 * casos: para el público, un servicio no visible no existe.
 */
public class OfertaNoDisponibleException extends RuntimeException {

    public OfertaNoDisponibleException(String id) {
        super("El servicio " + id + " no existe o no está disponible");
    }
}
