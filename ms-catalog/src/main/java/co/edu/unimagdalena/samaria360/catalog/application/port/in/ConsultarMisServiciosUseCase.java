package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ServicioNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;

import java.util.UUID;

/**
 * Flujo «Consultar mis servicios» del CU «Registrar servicio» (DEC-129, DEC-143): el prestador ve el estado de sus
 * servicios y el motivo de un rechazo. Solo los del claim {@code sub}.
 */
public interface ConsultarMisServiciosUseCase {

    int TAMANO_PAGINA = 10;

    /**
     * @param estado {@code Pendiente}, {@code Publicado}, {@code Rechazado} o nulo («todos»)
     */
    PaginaResultados<ServicioComercial> listar(UUID prestadorId, String estado, int pagina);

    /** @throws ServicioNoEncontradoException si no existe, el id no es válido o es de otro prestador */
    ServicioComercial detalle(UUID prestadorId, String id);
}
