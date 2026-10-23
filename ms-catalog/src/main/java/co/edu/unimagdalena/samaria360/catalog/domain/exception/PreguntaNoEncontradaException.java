package co.edu.unimagdalena.samaria360.catalog.domain.exception;

/** No existe una pregunta frecuente con ese id, o el id no es un ObjectId: el mismo 404 en los dos casos. */
public class PreguntaNoEncontradaException extends RuntimeException {

    public PreguntaNoEncontradaException(String id) {
        super("La pregunta frecuente " + id + " no existe");
    }
}
