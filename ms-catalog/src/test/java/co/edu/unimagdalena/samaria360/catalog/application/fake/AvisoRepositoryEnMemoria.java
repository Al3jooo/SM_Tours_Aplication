package co.edu.unimagdalena.samaria360.catalog.application.fake;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.AvisoRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Aviso;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Colección {@code avisos} en memoria, con el mismo orden (fecha e id descendentes) y el UPDATE condicional. */
public class AvisoRepositoryEnMemoria implements AvisoRepositoryPort {

    private static final Comparator<Aviso> RECIENTES_PRIMERO = Comparator.comparing(Aviso::fechaPublicacion)
            .thenComparing(Aviso::id).reversed();

    private final Map<String, Aviso> avisos = new LinkedHashMap<>();
    private long secuencia;

    public void agregar(Aviso aviso) {
        avisos.put(aviso.id(), aviso);
    }

    public int guardados() {
        return avisos.size();
    }

    @Override
    public String nuevoId() {
        return String.format("%024x", ++secuencia);
    }

    @Override
    public void insertar(Aviso aviso) {
        avisos.put(aviso.id(), aviso);
    }

    @Override
    public List<Aviso> activos() {
        return avisos.values().stream().filter(Aviso::activo).sorted(RECIENTES_PRIMERO).toList();
    }

    @Override
    public PaginaResultados<Aviso> pagina(int pagina, int tamano) {
        List<Aviso> todos = avisos.values().stream().sorted(RECIENTES_PRIMERO).toList();
        List<Aviso> pedazo = todos.stream().skip((long) pagina * tamano).limit(tamano).toList();
        return new PaginaResultados<>(pedazo, pagina, tamano, todos.size());
    }

    @Override
    public Optional<Aviso> buscar(String id) {
        return Optional.ofNullable(avisos.get(id));
    }

    @Override
    public boolean despublicar(String id) {
        Aviso aviso = avisos.get(id);
        if (aviso == null || !aviso.activo()) {
            return false;
        }
        avisos.put(id, new Aviso(id, aviso.mensaje(), aviso.tipo(), false, aviso.fechaPublicacion()));
        return true;
    }
}
