package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.GestionarFavoritosUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.FavoritoRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.OfertaReferenciadaPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.OfertaReferenciadaPort.Referencia;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.LimiteFavoritosException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.OfertaNoDisponibleException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Favorito;
import co.edu.unimagdalena.samaria360.catalog.domain.model.FavoritoResuelto;
import co.edu.unimagdalena.samaria360.catalog.domain.model.IdentificadorObjeto;
import co.edu.unimagdalena.samaria360.catalog.domain.model.TipoOferta;

import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * CU «Guardar favoritos» (RF54). El tope se cuenta antes de insertar, sin atomicidad (como LIMITE_FAQ): dos PUT
 * simultáneos de favoritos distintos pueden dejar uno de más. Dos PUT simultáneos del mismo favorito no lo duplican: el
 * índice único rechaza el segundo y se responde con el que ya existe (200, nunca 500).
 */
public class GestionarFavoritosService implements GestionarFavoritosUseCase {

    private final FavoritoRepositoryPort favoritos;
    private final OfertaReferenciadaPort referencias;
    private final RelojPort reloj;

    public GestionarFavoritosService(FavoritoRepositoryPort favoritos, OfertaReferenciadaPort referencias,
                                     RelojPort reloj) {
        this.favoritos = Objects.requireNonNull(favoritos);
        this.referencias = Objects.requireNonNull(referencias);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public List<FavoritoResuelto> listar(UUID turistaId) {
        return resolver(favoritos.listar(turistaId, Favorito.MAXIMO_POR_TURISTA));
    }

    /** La fecha va en milisegundos, como la guarda MongoDB. */
    @Override
    public Guardado guardar(UUID turistaId, TipoOferta tipo, String entidadId) {
        if (!IdentificadorObjeto.esValido(entidadId)) {
            throw new OfertaNoDisponibleException(entidadId);
        }
        Optional<Favorito> existente = favoritos.buscar(turistaId, tipo, entidadId);
        if (existente.isPresent()) {
            return new Guardado(resolver(List.of(existente.get())).get(0), false);
        }
        Referencia referencia = referencias.buscar(tipo, List.of(entidadId)).get(entidadId);
        if (referencia == null || !referencia.visible()) {
            throw new OfertaNoDisponibleException(entidadId);
        }
        if (favoritos.contar(turistaId) >= Favorito.MAXIMO_POR_TURISTA) {
            throw new LimiteFavoritosException();
        }
        Favorito nuevo = new Favorito(turistaId, tipo, entidadId, reloj.ahora().truncatedTo(ChronoUnit.MILLIS));
        if (!favoritos.insertar(nuevo)) {
            Favorito ganador = favoritos.buscar(turistaId, tipo, entidadId).orElse(nuevo);
            return new Guardado(resolver(List.of(ganador)).get(0), false);
        }
        return new Guardado(resuelto(nuevo, referencia), true);
    }

    @Override
    public void quitar(UUID turistaId, TipoOferta tipo, String entidadId) {
        if (IdentificadorObjeto.esValido(entidadId)) {
            favoritos.eliminar(turistaId, tipo, entidadId);
        }
    }

    /** Una consulta por tipo (servicios y atractivos), en el orden de la lista. */
    private List<FavoritoResuelto> resolver(List<Favorito> lista) {
        Map<TipoOferta, Map<String, Referencia>> porTipo = new EnumMap<>(TipoOferta.class);
        for (TipoOferta tipo : TipoOferta.values()) {
            List<String> ids = lista.stream().filter(f -> f.tipo() == tipo).map(Favorito::entidadId).toList();
            porTipo.put(tipo, ids.isEmpty() ? Map.of() : referencias.buscar(tipo, ids));
        }
        return lista.stream().map(f -> resuelto(f, porTipo.get(f.tipo()).get(f.entidadId()))).toList();
    }

    private static FavoritoResuelto resuelto(Favorito favorito, Referencia referencia) {
        if (referencia == null) {
            return FavoritoResuelto.sinReferencia(favorito);
        }
        return new FavoritoResuelto(favorito, referencia.nombre(), referencia.categoria(), referencia.imagen(),
                referencia.visible());
    }
}
