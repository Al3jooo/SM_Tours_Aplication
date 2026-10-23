package co.edu.unimagdalena.samaria360.catalog.domain.exception;

/**
 * El aviso ya estaba despublicado (DEC-64): 409 ESTADO_CAMBIADO con {@code estadoActual: Inactivo}, como una transición
 * de cuenta que ya ocurrió en ms-auth (DEC-39).
 */
public class AvisoYaInactivoException extends RuntimeException {

    public AvisoYaInactivoException(String id) {
        super("El aviso " + id + " ya estaba despublicado");
    }
}
