package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoPublicacion;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Revision;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioEditado;

import java.util.Optional;
import java.util.UUID;

/**
 * Colección {@code servicios} sin la regla de visibilidad pública: el registro (RF09), «Mis servicios» (DEC-143), la
 * edición del Prestador (RF32, RF43) y la aprobación del Administrador (RF21). La lectura pública sigue en {@link ServicioRepositoryPort}. Los ids son ObjectId
 * en texto (24 hexadecimales en minúscula), ya validados.
 */
public interface GestionServiciosPort {

    /** Un ObjectId nuevo, en su forma textual, para un servicio o una unidad que todavía no se guardó. */
    String nuevoId();

    void insertar(ServicioComercial servicio);

    /** El servicio en cualquier estado. */
    Optional<ServicioComercial> buscar(String id);

    /** Los del prestador, del más reciente al más antiguo; {@code estado} nulo es «todos». */
    PaginaResultados<ServicioComercial> listarDePrestador(UUID prestadorId, EstadoPublicacion estado, int pagina,
                                                          int tamano);

    /** Por estado, del más antiguo al más reciente: la cola de revisión del Administrador (DEC-144). */
    PaginaResultados<ServicioComercial> listarPorEstado(EstadoPublicacion estado, int pagina, int tamano);

    /**
     * UPDATE condicional (DEC-128, como DEC-39): pasa de Pendiente a {@code estadoNuevo} con su revisión solo si sigue
     * Pendiente, y sube la versión (DEC-186).
     *
     * @return false si no hay un servicio Pendiente con ese id (no existe o ya se decidió)
     */
    boolean decidir(String id, EstadoPublicacion estadoNuevo, Revision revision);

    /**
     * RF32: escribe {@code precioBase} y, en Alojamiento, las unidades con sus precios nuevos, y sube la versión. UPDATE
     * condicional sobre el id, el prestador, un estado editable (Pendiente o Publicado) y la versión esperada (DEC-186).
     *
     * @return false si ninguna de esas condiciones se cumple
     */
    boolean actualizarPrecio(ServicioEditado edicion);

    /** RF43: como {@link #actualizarPrecio}, con el nombre comercial y el contacto. */
    boolean actualizarDatosComerciales(ServicioEditado edicion);

    /** Fija {@code prestadorActivo} en un solo servicio (DEC-142); idempotente. */
    void fijarPrestadorActivoEnServicio(String id, boolean activo);
}
