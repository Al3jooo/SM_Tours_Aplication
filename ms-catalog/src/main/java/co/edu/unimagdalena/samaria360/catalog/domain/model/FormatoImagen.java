package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formatos de imagen que admite RF06: JPEG y PNG. Se reconocen por su firma (los primeros bytes del archivo), no por la
 * extensión ni por el tipo que declara el navegador (DEC-55, ADR-009).
 */
public enum FormatoImagen {
    JPEG("image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

    private final String tipoContenido;
    private final byte[] firma;

    FormatoImagen(String tipoContenido, byte[] firma) {
        this.tipoContenido = tipoContenido;
        this.firma = firma;
    }

    /** El tipo MIME con el que se sirve la imagen. */
    public String tipoContenido() {
        return tipoContenido;
    }

    /** El formato cuya firma coincide con el comienzo del contenido; vacío si no es JPEG ni PNG. */
    public static Optional<FormatoImagen> detectar(byte[] contenido) {
        if (contenido == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(formato -> contenido.length >= formato.firma.length
                        && Arrays.equals(contenido, 0, formato.firma.length, formato.firma, 0, formato.firma.length))
                .findFirst();
    }
}
