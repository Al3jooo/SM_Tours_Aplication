package co.edu.unimagdalena.samaria360.catalog.domain.exception;

/** Los criterios de búsqueda no cumplen las reglas de RF16, RF38 o RF61. */
public class CriteriosBusquedaInvalidosException extends RuntimeException {

    public CriteriosBusquedaInvalidosException(String motivo) {
        super(motivo);
    }
}
