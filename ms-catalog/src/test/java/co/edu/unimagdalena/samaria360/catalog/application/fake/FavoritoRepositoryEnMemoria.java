package co.edu.unimagdalena.samaria360.catalog.application.fake;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.FavoritoRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Favorito;
import co.edu.unimagdalena.samaria360.catalog.domain.model.TipoOferta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Favoritos en memoria con el índice único de M007. {@link #simularCarrera} hace que la próxima búsqueda no lo vea y
 * que la inserción choque con el índice, como si otro PUT simultáneo lo hubiera guardado antes.
 */
public class FavoritoRepositoryEnMemoria implements FavoritoRepositoryPort {

    private final List<Favorito> favoritos = new ArrayList<>();
    private Favorito ganadorDeLaCarrera;

    public void agregar(Favorito favorito) {
        favoritos.add(favorito);
    }

    public void simularCarrera(Favorito ganador) {
        this.ganadorDeLaCarrera = ganador;
    }

    public List<Favorito> todos() {
        return List.copyOf(favoritos);
    }

    @Override
    public Optional<Favorito> buscar(UUID turistaId, TipoOferta tipo, String entidadId) {
        return favoritos.stream().filter(f -> mismo(f, turistaId, tipo, entidadId)).findFirst();
    }

    @Override
    public List<Favorito> listar(UUID turistaId, int maximo) {
        return favoritos.stream().filter(f -> f.turistaId().equals(turistaId))
                .sorted(Comparator.comparing(Favorito::fechaCreacion).reversed()).limit(maximo).toList();
    }

    @Override
    public long contar(UUID turistaId) {
        return favoritos.stream().filter(f -> f.turistaId().equals(turistaId)).count();
    }

    @Override
    public boolean insertar(Favorito favorito) {
        if (ganadorDeLaCarrera != null) {
            favoritos.add(ganadorDeLaCarrera);
            ganadorDeLaCarrera = null;
            return false;
        }
        if (buscar(favorito.turistaId(), favorito.tipo(), favorito.entidadId()).isPresent()) {
            return false;
        }
        favoritos.add(favorito);
        return true;
    }

    @Override
    public void eliminar(UUID turistaId, TipoOferta tipo, String entidadId) {
        favoritos.removeIf(f -> mismo(f, turistaId, tipo, entidadId));
    }

    private static boolean mismo(Favorito f, UUID turistaId, TipoOferta tipo, String entidadId) {
        return f.turistaId().equals(turistaId) && f.tipo() == tipo && f.entidadId().equals(entidadId);
    }
}
