package co.edu.unimagdalena.samaria360.catalog.domain.exception;

/** No existe un atractivo con ese id, o el id no es un ObjectId: el mismo 404 en los dos casos (DEC-217). */
public class AtractivoNoEncontradoException extends RuntimeException {

    public AtractivoNoEncontradoException(String id) {
        super("El atractivo " + id + " no existe");
    }
}
