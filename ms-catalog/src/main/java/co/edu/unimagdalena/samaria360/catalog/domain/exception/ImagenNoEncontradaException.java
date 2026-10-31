package co.edu.unimagdalena.samaria360.catalog.domain.exception;

/** No existe una imagen con ese id, o el id no es un ObjectId: el mismo 404 en los dos casos (DEC-209). */
public class ImagenNoEncontradaException extends RuntimeException {

    public ImagenNoEncontradaException(String id) {
        super("La imagen " + id + " no existe");
    }
}
