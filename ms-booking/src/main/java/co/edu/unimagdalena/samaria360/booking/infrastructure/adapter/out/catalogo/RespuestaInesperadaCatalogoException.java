package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.out.catalogo;

/**
 * ms-catalog respondió algo que no corresponde al contrato (un 4xx distinto de 404 o un cuerpo inesperado). No es
 * indisponibilidad: no se reintenta, el circuito la ignora y termina en 500 {@code ERROR_INTERNO}.
 */
public class RespuestaInesperadaCatalogoException extends RuntimeException {

    public RespuestaInesperadaCatalogoException(String mensaje) {
        super(mensaje);
    }
}
