package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import co.edu.unimagdalena.samaria360.catalog.domain.model.Favorito;
import co.edu.unimagdalena.samaria360.catalog.domain.model.TipoOferta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Colección {@code favoritos} de catalog_db (M007). */
public interface FavoritoRepositoryPort {

    Optional<Favorito> buscar(UUID turistaId, TipoOferta tipo, String entidadId);

    /** Del más reciente al más antiguo. */
    List<Favorito> listar(UUID turistaId, int maximo);

    long contar(UUID turistaId);

    /** @return false si ya existía: el índice único lo rechazó (dos PUT simultáneos del mismo favorito) */
    boolean insertar(Favorito favorito);

    void eliminar(UUID turistaId, TipoOferta tipo, String entidadId);
}
