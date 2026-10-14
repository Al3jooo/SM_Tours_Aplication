package co.edu.unimagdalena.samaria360.auth.domain.exception;

/**
 * El registro no aceptó la Política de Privacidad (RF01; para el prestador, requisito derivado de E1 §3.2.7.4 y WF-03).
 * Misma regla y mismo código para turista y prestador (400 POLITICAS_NO_ACEPTADAS).
 */
public class PoliticasNoAceptadasException extends RuntimeException {

    public PoliticasNoAceptadasException() {
        super("Debe aceptar la Política de Privacidad y el tratamiento de datos");
    }
}
