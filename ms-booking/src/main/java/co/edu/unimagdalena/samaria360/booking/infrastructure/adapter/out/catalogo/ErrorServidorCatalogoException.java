package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.out.catalogo;

/** ms-catalog respondió 5xx: se reintenta una vez y cuenta como fallo para el circuito. */
class ErrorServidorCatalogoException extends RuntimeException {

    ErrorServidorCatalogoException(int estado) {
        super("ms-catalog respondió " + estado);
    }
}
