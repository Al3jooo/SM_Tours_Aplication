package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Campos del cuerpo que no sirven: nombre JSON → código (nunca textos), como en ms-auth (400 DATOS_INVALIDOS). */
public class DatosInvalidosException extends RuntimeException {

    private final Map<String, String> campos;

    public DatosInvalidosException(Map<String, String> campos) {
        super("Datos de entrada inválidos: " + campos.keySet());
        this.campos = Collections.unmodifiableMap(new LinkedHashMap<>(campos));
    }

    public Map<String, String> getCampos() {
        return campos;
    }
}
