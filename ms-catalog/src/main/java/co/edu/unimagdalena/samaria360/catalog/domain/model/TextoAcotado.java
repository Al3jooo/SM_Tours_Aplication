package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.Optional;

/**
 * Texto obligatorio con un máximo de caracteres, contados después de quitar los espacios de los extremos con
 * {@link String#strip()} (DEC-61, como DEC-42). Los caracteres son puntos de código, que es lo que mide {@code maxLength}
 * en el validador de MongoDB: un emoji cuenta uno, no dos.
 */
public final class TextoAcotado {

    private TextoAcotado() {
    }

    /** El texto sin los espacios de los extremos; un nulo es un texto vacío. */
    public static String limpiar(String texto) {
        return texto == null ? "" : texto.strip();
    }

    /** Motivo por el que el texto no sirve; vacío si es válido. */
    public static Optional<MotivoInvalido> validar(String texto, int maximo) {
        String limpio = limpiar(texto);
        if (limpio.isEmpty()) {
            return Optional.of(MotivoInvalido.REQUERIDO);
        }
        if (limpio.codePointCount(0, limpio.length()) > maximo) {
            return Optional.of(MotivoInvalido.LONGITUD_MAXIMA);
        }
        return Optional.empty();
    }

    /** Para los constructores: el texto ya debe venir limpio y dentro del máximo. */
    static String exigir(String texto, int maximo, String campo) {
        if (texto == null || !texto.equals(limpiar(texto)) || validar(texto, maximo).isPresent()) {
            throw new IllegalArgumentException(campo + " debe tener de 1 a " + maximo
                    + " caracteres, sin espacios en los extremos");
        }
        return texto;
    }
}
