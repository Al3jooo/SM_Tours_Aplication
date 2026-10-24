package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Servicio o atractivo que un turista marcó como favorito (RF54, DEC-80). Solo guarda la referencia: el nombre, la
 * categoría, la imagen y la disponibilidad se resuelven al listar, sin desnormalizar (DEC-86).
 *
 * @param entidadId _id del servicio o del atractivo, como ObjectId en texto
 */
public record Favorito(UUID turistaId, TipoOferta tipo, String entidadId, Instant fechaCreacion) {

    /** Tope por turista (DEC-86): el SRS no lo define; «Mis favoritos» y los corazones de WF-05 piden la lista entera. */
    public static final int MAXIMO_POR_TURISTA = 100;

    public Favorito {
        Objects.requireNonNull(turistaId, "turistaId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(fechaCreacion, "fechaCreacion");
        if (!IdentificadorObjeto.esValido(entidadId)) {
            throw new IllegalArgumentException("entidadId no es un ObjectId: " + entidadId);
        }
    }
}
