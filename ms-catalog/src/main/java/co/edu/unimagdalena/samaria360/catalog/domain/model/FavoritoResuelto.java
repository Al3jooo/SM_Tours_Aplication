package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.Objects;

/**
 * Un favorito con los datos actuales de lo que referencia (DEC-86). Un favorito que deja de estar visible no se borra:
 * se lista con {@code disponible = false}; si el documento ya no existe, nombre, categoría e imagen van en null.
 *
 * @param imagen     la primera de {@code imagenes} (la portada, RF48), o null
 * @param disponible un servicio Publicado de un prestador activo (RF16, RF22) o un atractivo que existe
 */
public record FavoritoResuelto(Favorito favorito, String nombre, String categoria, String imagen, boolean disponible) {

    public FavoritoResuelto {
        Objects.requireNonNull(favorito, "favorito");
    }

    /** El documento referenciado ya no existe. */
    public static FavoritoResuelto sinReferencia(Favorito favorito) {
        return new FavoritoResuelto(favorito, null, null, null, false);
    }
}
