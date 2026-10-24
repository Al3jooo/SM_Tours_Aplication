package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.FavoritoResuelto;

import java.time.Instant;

/**
 * Un favorito de «Mis favoritos» (RF54, DEC-86). {@code nombre}, {@code categoria} e {@code imagen} son null si lo que
 * referencia ya no existe; {@code disponible} es false si ya no está visible.
 */
public record RespuestaFavorito(String tipo, String id, String nombre, String categoria, String imagen,
                                Instant fechaCreacion, boolean disponible) {

    public static RespuestaFavorito de(FavoritoResuelto resuelto) {
        return new RespuestaFavorito(resuelto.favorito().tipo().name(), resuelto.favorito().entidadId(),
                resuelto.nombre(), resuelto.categoria(), resuelto.imagen(), resuelto.favorito().fechaCreacion(),
                resuelto.disponible());
    }
}
