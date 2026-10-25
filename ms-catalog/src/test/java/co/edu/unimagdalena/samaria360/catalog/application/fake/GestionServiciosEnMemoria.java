package co.edu.unimagdalena.samaria360.catalog.application.fake;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.GestionServiciosPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.VisibilidadPrestadorPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoPublicacion;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Revision;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Unidades;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Colección {@code servicios} en memoria con la semántica del adaptador MongoDB: el UPDATE condicional sobre Pendiente,
 * los órdenes de los listados y {@code prestadorActivo} por servicio o por prestador ({@link #visibilidad()} es el
 * {@link VisibilidadPrestadorPort} de RF22, para probar la carrera de DEC-142 con los dos casos de uso).
 */
public class GestionServiciosEnMemoria implements GestionServiciosPort {

    private static final Comparator<ServicioComercial> ANTIGUOS_PRIMERO = Comparator
            .comparing((ServicioComercial s) -> s.getFechaRegistro().orElseThrow())
            .thenComparing(s -> s.getId().orElseThrow());

    private final Map<String, ServicioComercial> servicios = new LinkedHashMap<>();

    public void agregar(ServicioComercial servicio) {
        servicios.put(servicio.getId().orElseThrow(), servicio);
    }

    public ServicioComercial guardado(String id) {
        return servicios.get(id);
    }

    public List<ServicioComercial> todos() {
        return List.copyOf(servicios.values());
    }

    @Override
    public String nuevoId() {
        return Unidades.nuevoId();
    }

    @Override
    public void insertar(ServicioComercial servicio) {
        agregar(servicio);
    }

    @Override
    public Optional<ServicioComercial> buscar(String id) {
        return Optional.ofNullable(servicios.get(id));
    }

    @Override
    public PaginaResultados<ServicioComercial> listarDePrestador(UUID prestadorId, EstadoPublicacion estado,
                                                                 int pagina, int tamano) {
        return pagina(servicios.values().stream()
                .filter(s -> s.getPrestadorId().equals(prestadorId))
                .filter(s -> estado == null || s.getEstadoPublicacion() == estado)
                .sorted(ANTIGUOS_PRIMERO.reversed()).toList(), pagina, tamano);
    }

    @Override
    public PaginaResultados<ServicioComercial> listarPorEstado(EstadoPublicacion estado, int pagina, int tamano) {
        return pagina(servicios.values().stream().filter(s -> s.getEstadoPublicacion() == estado)
                .sorted(ANTIGUOS_PRIMERO).toList(), pagina, tamano);
    }

    @Override
    public boolean decidir(String id, EstadoPublicacion estadoNuevo, Revision revision) {
        ServicioComercial actual = servicios.get(id);
        if (actual == null || actual.getEstadoPublicacion() != EstadoPublicacion.Pendiente) {
            return false;
        }
        agregar(new ServicioComercial(id, actual.getPrestadorId(), actual.getNombreComercial(), actual.getCategoria(),
                actual.getDetalle(), actual.getDescripcion().orElse(null), actual.getCoordenada(),
                actual.getContacto().orElse(null), estadoNuevo, actual.isPrestadorActivo(),
                actual.getCondicionesAccesibilidad(), actual.getHorarioAtencion().orElse(null),
                actual.getPrecioBase().orElse(null), actual.getImagenes(), actual.getDireccion().orElse(null),
                actual.getOferta().orElse(null), actual.getRnt().orElse(null),
                actual.getFechaRegistro().orElse(null), revision));
        return true;
    }

    @Override
    public void fijarPrestadorActivoEnServicio(String id, boolean activo) {
        buscar(id).ifPresent(servicio -> agregar(servicio.conPrestadorActivo(activo)));
    }

    /** RF22: el {@code updateMulti} por prestador, sobre estos mismos servicios. */
    public VisibilidadPrestadorPort visibilidad() {
        return (idPrestador, activo) -> new ArrayList<>(servicios.values()).stream()
                .filter(s -> s.getPrestadorId().toString().equals(idPrestador))
                .forEach(s -> agregar(s.conPrestadorActivo(activo)));
    }

    private static PaginaResultados<ServicioComercial> pagina(List<ServicioComercial> lista, int pagina, int tamano) {
        int desde = Math.min(pagina * tamano, lista.size());
        return new PaginaResultados<>(lista.subList(desde, Math.min(desde + tamano, lista.size())), pagina, tamano,
                lista.size());
    }
}
