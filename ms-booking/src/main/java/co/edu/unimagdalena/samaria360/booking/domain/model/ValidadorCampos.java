package co.edu.unimagdalena.samaria360.booking.domain.model;

import co.edu.unimagdalena.samaria360.booking.domain.exception.DatosReservaInvalidosException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Junta un código por campo y lanza todos a la vez (400 {@code DATOS_INVALIDOS}), como el registro de ms-auth. */
public final class ValidadorCampos {

    private final Map<String, MotivoInvalido> campos = new LinkedHashMap<>();

    /** Solo el primer error de cada campo. */
    public void error(String campo, MotivoInvalido motivo) {
        campos.putIfAbsent(campo, motivo);
    }

    public boolean tieneError(String campo) {
        return campos.containsKey(campo);
    }

    public void lanzarSiHayErrores() {
        if (!campos.isEmpty()) {
            throw new DatosReservaInvalidosException(campos);
        }
    }

    /** null o vacío → null; si no, el texto sin espacios a los lados. */
    public static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    /** Fecha ISO (aaaa-mm-dd), o null si falta o no tiene formato (y entonces queda registrado el error). */
    public LocalDate fecha(String campo, String valor, boolean requerida) {
        String limpio = texto(valor);
        if (limpio == null) {
            if (requerida) {
                error(campo, MotivoInvalido.REQUERIDO);
            }
            return null;
        }
        try {
            return LocalDate.parse(limpio);
        } catch (DateTimeParseException e) {
            error(campo, MotivoInvalido.FORMATO);
            return null;
        }
    }

    /** Hora ISO (hh:mm o hh:mm:ss), o null si falta o no tiene formato. */
    public LocalTime hora(String campo, String valor) {
        String limpio = texto(valor);
        if (limpio == null) {
            return null;
        }
        try {
            return LocalTime.parse(limpio);
        } catch (DateTimeParseException e) {
            error(campo, MotivoInvalido.FORMATO);
            return null;
        }
    }

    /** Id de catalog_db: 24 hexadecimales. */
    public String objectId(String campo, String valor, boolean requerido) {
        String limpio = texto(valor);
        if (limpio == null) {
            if (requerido) {
                error(campo, MotivoInvalido.REQUERIDO);
            }
            return null;
        }
        if (!IdentificadorObjeto.esValido(limpio)) {
            error(campo, MotivoInvalido.FORMATO);
            return null;
        }
        return limpio;
    }
}
