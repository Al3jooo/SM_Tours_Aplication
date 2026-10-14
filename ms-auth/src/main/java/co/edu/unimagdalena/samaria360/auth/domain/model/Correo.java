package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.ValorInvalidoException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Correo de acceso, normalizado: sin espacios alrededor y en minúsculas. El registro y el inicio de sesión usan la misma
 * normalización, así que «Ana@X.com» y «ana@x.com» son la misma cuenta.
 *
 * @param valor ya normalizado
 */
public record Correo(String valor) {

    /** Como {@code usuario.correo VARCHAR(100)} en V1. */
    public static final int LONGITUD_MAXIMA = 100;

    /** Forma básica: algo, «@», dominio con un punto; sin espacios. La comprobación real sería enviar un correo (RF34). */
    private static final Pattern FORMA = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public Correo {
        if (valor == null || valor.isBlank()) {
            throw new ValorInvalidoException(MotivoInvalido.REQUERIDO);
        }
        valor = normalizar(valor);
        if (valor.length() > LONGITUD_MAXIMA) {
            throw new ValorInvalidoException(MotivoInvalido.LONGITUD_MAXIMA);
        }
        if (!FORMA.matcher(valor).matches()) {
            throw new ValorInvalidoException(MotivoInvalido.FORMATO);
        }
    }

    /** Normalización compartida con el inicio de sesión (RF15). */
    public static String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
