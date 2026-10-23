package co.edu.unimagdalena.samaria360.catalog.domain.exception;

import co.edu.unimagdalena.samaria360.catalog.domain.model.MotivoInvalido;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Campos de una pregunta frecuente o de un aviso que no sirven: nombre JSON → motivo (400 DATOS_INVALIDOS). */
public class ContenidoInvalidoException extends RuntimeException {

    private final Map<String, MotivoInvalido> campos;

    public ContenidoInvalidoException(Map<String, MotivoInvalido> campos) {
        super("Datos de entrada inválidos: " + campos.keySet());
        this.campos = Collections.unmodifiableMap(new LinkedHashMap<>(campos));
    }

    public Map<String, MotivoInvalido> getCampos() {
        return campos;
    }
}
