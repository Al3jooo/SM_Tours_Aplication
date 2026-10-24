package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.LimiteFavoritosException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.OfertaNoDisponibleException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.FavoritoResuelto;
import co.edu.unimagdalena.samaria360.catalog.domain.model.TipoOferta;

import java.util.List;
import java.util.UUID;

/** CU «Guardar favoritos» (RF54, DEC-80, DEC-86): solo del rol Turista, siempre el del claim {@code sub}. */
public interface GestionarFavoritosUseCase {

    /** «Mis favoritos»: todos (como máximo 100), del más reciente al más antiguo, con su disponibilidad actual. */
    List<FavoritoResuelto> listar(UUID turistaId);

    /**
     * Idempotente: un favorito que ya existe no se duplica y se devuelve aunque ya no esté disponible.
     *
     * @throws OfertaNoDisponibleException si el id no es un ObjectId, no existe o no es visible
     * @throws LimiteFavoritosException    si el turista ya tiene 100
     */
    Guardado guardar(UUID turistaId, TipoOferta tipo, String entidadId);

    /** Idempotente: quitar uno que no existe no es un error. */
    void quitar(UUID turistaId, TipoOferta tipo, String entidadId);

    /** @param creado true si se agregó ahora (201); false si ya estaba (200) */
    record Guardado(FavoritoResuelto favorito, boolean creado) {
    }
}
