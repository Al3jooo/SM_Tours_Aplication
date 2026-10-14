package co.edu.unimagdalena.samaria360.auth.domain.exception;

/**
 * Ya existe un prestador con ese RNT (409 RNT_YA_REGISTRADO). Se puede revelar porque el Registro Nacional de Turismo es
 * público (ADR-006); la comprobación no depende del correo.
 */
public class RntYaRegistradoException extends RuntimeException {

    public RntYaRegistradoException() {
        super("Ya existe un prestador registrado con ese RNT");
    }
}
