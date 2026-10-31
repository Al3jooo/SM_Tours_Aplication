package co.edu.unimagdalena.samaria360.catalog.application.fake;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.AtractivoRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.GestionAtractivosPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Colección {@code atractivos} en memoria con la semántica del adaptador MongoDB: la escritura condicional sobre la
 * versión leída, que la sube en uno (DEC-207), y el orden del panel. {@link #fallarAlInsertar(RuntimeException)} simula
 * un insert que falla (compensación de las imágenes, ADR-009) y {@link #antesDeEscribir(Runnable)} lo que pasa entre la
 * lectura y la escritura de una edición.
 */
public class GestionAtractivosEnMemoria implements GestionAtractivosPort, AtractivoRepositoryPort {

    private static final Comparator<AtractivoPublico> RECIENTES_PRIMERO = Comparator
            .comparing((AtractivoPublico a) -> a.getFechaRegistro().orElseThrow()).reversed()
            .thenComparing(a -> a.getId().orElseThrow(), Comparator.reverseOrder());

    private final Map<String, AtractivoPublico> atractivos = new LinkedHashMap<>();
    private int siguienteId = 1;
    private RuntimeException errorAlInsertar;
    private Runnable antesDeEscribir = () -> {
    };

    public void agregar(AtractivoPublico atractivo) {
        atractivos.put(atractivo.getId().orElseThrow(), atractivo);
    }

    public AtractivoPublico guardado(String id) {
        return atractivos.get(id);
    }

    public List<AtractivoPublico> todos() {
        return List.copyOf(atractivos.values());
    }

    public void fallarAlInsertar(RuntimeException error) {
        this.errorAlInsertar = error;
    }

    public void antesDeEscribir(Runnable accion) {
        this.antesDeEscribir = accion;
    }

    @Override
    public String nuevoId() {
        return String.format("%024x", siguienteId++);
    }

    @Override
    public void insertar(AtractivoPublico atractivo) {
        if (errorAlInsertar != null) {
            throw errorAlInsertar;
        }
        agregar(atractivo);
    }

    @Override
    public Optional<AtractivoPublico> buscar(String id) {
        return Optional.ofNullable(atractivos.get(id));
    }

    @Override
    public PaginaResultados<AtractivoPublico> listar(int pagina, int tamano) {
        List<AtractivoPublico> ordenados = atractivos.values().stream().sorted(RECIENTES_PRIMERO).toList();
        List<AtractivoPublico> elementos = ordenados.stream().skip((long) pagina * tamano).limit(tamano).toList();
        return new PaginaResultados<>(elementos, pagina, tamano, ordenados.size());
    }

    @Override
    public boolean actualizar(AtractivoPublico atractivo) {
        return escribir(atractivo);
    }

    @Override
    public boolean cambiarEstado(AtractivoPublico atractivo) {
        return escribir(atractivo);
    }

    private boolean escribir(AtractivoPublico editado) {
        antesDeEscribir.run();
        String id = editado.getId().orElseThrow();
        AtractivoPublico actual = atractivos.get(id);
        if (actual == null || actual.getVersion() != editado.getVersion()) {
            return false;
        }
        atractivos.put(id, conVersion(editado, editado.getVersion() + 1));
        return true;
    }

    /** El mismo atractivo con otra versión (p. ej. una edición de otro Gestor). */
    public static AtractivoPublico conVersion(AtractivoPublico a, int version) {
        return new AtractivoPublico(a.getId().orElse(null), a.getGestorId(), a.getNombre(), a.getCoordenada(),
                a.getDescripcion().orElse(null), a.getCategoria().orElse(null), a.getImagenes(),
                a.getLimiteCapacidadCarga(), a.getEstadoOperativo().orElse(null),
                a.getNormasAmbientales().orElse(null), a.getCondicionesAccesibilidad(),
                a.getHorarioAtencion().orElse(null), a.getFechaRegistro().orElse(null),
                a.getFechaActualizacion().orElse(null), version);
    }
}
