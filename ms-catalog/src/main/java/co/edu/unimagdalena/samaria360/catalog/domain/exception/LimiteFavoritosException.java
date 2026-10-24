package co.edu.unimagdalena.samaria360.catalog.domain.exception;

import co.edu.unimagdalena.samaria360.catalog.domain.model.Favorito;

/** El turista ya tiene {@value Favorito#MAXIMO_POR_TURISTA} favoritos (DEC-86): 409 LIMITE_FAVORITOS. */
public class LimiteFavoritosException extends RuntimeException {

    public LimiteFavoritosException() {
        super("Ya tienes " + Favorito.MAXIMO_POR_TURISTA + " favoritos, el máximo permitido");
    }
}
