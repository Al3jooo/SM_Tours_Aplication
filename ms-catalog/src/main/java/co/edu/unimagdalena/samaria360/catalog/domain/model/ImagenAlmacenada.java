package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.Objects;

/**
 * Una imagen ya guardada (GridFS, ADR-009), lista para servirla.
 *
 * @param formato el que se detectó al registrarla
 */
public record ImagenAlmacenada(FormatoImagen formato, byte[] contenido) {

    public ImagenAlmacenada {
        Objects.requireNonNull(formato, "formato");
        Objects.requireNonNull(contenido, "contenido");
    }
}
