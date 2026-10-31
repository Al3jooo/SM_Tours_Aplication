package co.edu.unimagdalena.samaria360.catalog.domain.model;

/**
 * Un archivo recibido en el registro de un atractivo (RF06), sin validar.
 *
 * @param nombre    el nombre original, solo informativo (se guarda en GridFS; nunca decide el formato)
 * @param contenido los bytes completos; nulo o vacío si la parte llegó sin archivo
 */
public record ImagenSubida(String nombre, byte[] contenido) {
}
