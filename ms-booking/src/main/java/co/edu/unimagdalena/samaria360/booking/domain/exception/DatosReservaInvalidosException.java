package co.edu.unimagdalena.samaria360.booking.domain.exception;

import co.edu.unimagdalena.samaria360.booking.domain.model.MotivoInvalido;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Uno o más campos inválidos (400 {@code DATOS_INVALIDOS}): un código por campo, con los nombres JSON de la API, todos
 * a la vez y en el orden en que se validaron.
 */
public class DatosReservaInvalidosException extends RuntimeException {

    private final Map<String, MotivoInvalido> campos;

    public DatosReservaInvalidosException(Map<String, MotivoInvalido> campos) {
        super("Datos de entrada inválidos: " + campos);
        this.campos = new LinkedHashMap<>(campos);
    }

    /** En el orden de validación. */
    public Map<String, MotivoInvalido> getCampos() {
        return Collections.unmodifiableMap(campos);
    }
}
