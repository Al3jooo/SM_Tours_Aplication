package co.edu.unimagdalena.samaria360.catalog.domain.exception;

/**
 * No existe un servicio con ese id, el id no es un ObjectId o, en «Mis servicios», el servicio es de otro prestador: el
 * mismo 404 en todos los casos (DEC-143, DEC-151).
 */
public class ServicioNoEncontradoException extends RuntimeException {

    public ServicioNoEncontradoException(String id) {
        super("El servicio " + id + " no existe");
    }
}
