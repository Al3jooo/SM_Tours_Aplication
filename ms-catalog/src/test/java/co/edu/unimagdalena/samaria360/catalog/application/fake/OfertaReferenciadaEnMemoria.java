package co.edu.unimagdalena.samaria360.catalog.application.fake;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.OfertaReferenciadaPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.TipoOferta;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Servicios y atractivos en memoria, con su visibilidad; cuenta las consultas para verificar que van por tipo. */
public class OfertaReferenciadaEnMemoria implements OfertaReferenciadaPort {

    private final Map<TipoOferta, Map<String, Referencia>> referencias = new EnumMap<>(TipoOferta.class);
    private int consultas;

    public void agregar(TipoOferta tipo, String id, Referencia referencia) {
        referencias.computeIfAbsent(tipo, t -> new HashMap<>()).put(id, referencia);
    }

    public void quitar(TipoOferta tipo, String id) {
        referencias.getOrDefault(tipo, new HashMap<>()).remove(id);
    }

    public int consultas() {
        return consultas;
    }

    @Override
    public Map<String, Referencia> buscar(TipoOferta tipo, Collection<String> ids) {
        consultas++;
        Map<String, Referencia> encontradas = new HashMap<>();
        Map<String, Referencia> delTipo = referencias.getOrDefault(tipo, Map.of());
        ids.stream().filter(delTipo::containsKey).forEach(id -> encontradas.put(id, delTipo.get(id)));
        return encontradas;
    }
}
