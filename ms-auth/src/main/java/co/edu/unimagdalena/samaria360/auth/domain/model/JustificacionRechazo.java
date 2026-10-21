package co.edu.unimagdalena.samaria360.auth.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Justificación obligatoria del rechazo de un prestador (RF28): de 1 a 500 caracteres una vez quitados los espacios de
 * los extremos (DEC-42).
 */
public record JustificacionRechazo(String texto) {

    public static final int LONGITUD_MAXIMA = 500;

    public JustificacionRechazo {
        Objects.requireNonNull(texto, "texto");
        if (texto.isEmpty() || texto.length() > LONGITUD_MAXIMA) {
            throw new IllegalArgumentException("La justificación debe tener de 1 a " + LONGITUD_MAXIMA + " caracteres");
        }
    }

    /** Motivo por el que el texto no sirve; vacío si es válido. */
    public static Optional<MotivoInvalido> validar(String texto) {
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.isEmpty()) {
            return Optional.of(MotivoInvalido.REQUERIDO);
        }
        if (limpio.length() > LONGITUD_MAXIMA) {
            return Optional.of(MotivoInvalido.LONGITUD_MAXIMA);
        }
        return Optional.empty();
    }

    /** @throws IllegalArgumentException si {@link #validar} devolvería un motivo */
    public static JustificacionRechazo de(String texto) {
        return new JustificacionRechazo(texto == null ? "" : texto.strip());
    }
}
