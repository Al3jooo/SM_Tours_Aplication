package co.edu.unimagdalena.samaria360.auth.application.port.out;

/** El destino no aceptó la entrega (red, tiempo agotado o respuesta que no es 2xx); el relevo la reintenta. */
public class EntregaFallidaException extends RuntimeException {

    public EntregaFallidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public EntregaFallidaException(String mensaje) {
        super(mensaje);
    }
}
