package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.ValorInvalidoException;

import java.nio.charset.StandardCharsets;

/**
 * Contraseña elegida al registrarse (RF01, RF02): al menos 8 caracteres (WF-02, WF-03) y como máximo 72 bytes en UTF-8,
 * el límite de BCrypt (RNF05): más allá, BCrypt ignoraría el resto y Spring Security la rechaza. No se recorta.
 */
public record ContrasenaNueva(String valor) {

    public static final int LONGITUD_MINIMA = 8;
    public static final int BYTES_MAXIMOS = 72;

    public ContrasenaNueva {
        if (valor == null || valor.isEmpty()) {
            throw new ValorInvalidoException(MotivoInvalido.REQUERIDO);
        }
        if (valor.codePointCount(0, valor.length()) < LONGITUD_MINIMA) {
            throw new ValorInvalidoException(MotivoInvalido.LONGITUD_MINIMA);
        }
        if (valor.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS) {
            throw new ValorInvalidoException(MotivoInvalido.LONGITUD_MAXIMA);
        }
    }

    /** Nunca aparece en logs. */
    @Override
    public String toString() {
        return "ContrasenaNueva[****]";
    }
}
