package co.edu.unimagdalena.samaria360.catalog.domain.exception;

import co.edu.unimagdalena.samaria360.catalog.domain.model.PreguntaFrecuente;

/** Ya hay {@value PreguntaFrecuente#MAXIMO_PREGUNTAS} preguntas frecuentes (DEC-62): 409 LIMITE_FAQ. */
public class LimitePreguntasException extends RuntimeException {

    public LimitePreguntasException() {
        super("Ya hay " + PreguntaFrecuente.MAXIMO_PREGUNTAS + " preguntas frecuentes, el máximo permitido");
    }
}
