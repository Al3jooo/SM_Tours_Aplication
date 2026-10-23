package co.edu.unimagdalena.samaria360.catalog.domain.exception;

/** No existe un aviso con ese id, o el id no es un ObjectId: el mismo 404 en los dos casos. */
public class AvisoNoEncontradoException extends RuntimeException {

    public AvisoNoEncontradoException(String id) {
        super("El aviso " + id + " no existe");
    }
}
