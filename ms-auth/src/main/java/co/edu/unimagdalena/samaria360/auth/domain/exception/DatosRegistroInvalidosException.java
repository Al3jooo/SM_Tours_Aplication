package co.edu.unimagdalena.samaria360.auth.domain.exception;

import co.edu.unimagdalena.samaria360.auth.domain.model.MotivoInvalido;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Uno o más campos del registro no son válidos: un motivo por campo (400 DATOS_INVALIDOS). */
public class DatosRegistroInvalidosException extends RuntimeException {

    private final Map<String, MotivoInvalido> campos;

    public DatosRegistroInvalidosException(Map<String, MotivoInvalido> campos) {
        super("Datos de registro inválidos: " + campos.keySet());
        this.campos = Collections.unmodifiableMap(new LinkedHashMap<>(campos));
    }

    /** Campo (nombre JSON) → motivo, en el orden del formulario. */
    public Map<String, MotivoInvalido> getCampos() {
        return campos;
    }
}
