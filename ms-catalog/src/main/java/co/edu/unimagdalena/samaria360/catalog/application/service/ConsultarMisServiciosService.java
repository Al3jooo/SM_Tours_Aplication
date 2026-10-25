package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarMisServiciosUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.GestionServiciosPort;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ServicioNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoPublicacion;
import co.edu.unimagdalena.samaria360.catalog.domain.model.IdentificadorObjeto;
import co.edu.unimagdalena.samaria360.catalog.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** «Consultar mis servicios» (DEC-129, DEC-143): un servicio de otro prestador es, para este, inexistente (404). */
public class ConsultarMisServiciosService implements ConsultarMisServiciosUseCase {

    private final GestionServiciosPort servicios;

    public ConsultarMisServiciosService(GestionServiciosPort servicios) {
        this.servicios = Objects.requireNonNull(servicios);
    }

    @Override
    public PaginaResultados<ServicioComercial> listar(UUID prestadorId, String estado, int pagina) {
        if (pagina < 0) {
            throw new ContenidoInvalidoException(Map.of("pagina", MotivoInvalido.FORMATO));
        }
        EstadoPublicacion filtro = estado == null || estado.isBlank() ? null : estadoPublicacion(estado);
        return servicios.listarDePrestador(prestadorId, filtro, pagina, TAMANO_PAGINA);
    }

    @Override
    public ServicioComercial detalle(UUID prestadorId, String id) {
        if (!IdentificadorObjeto.esValido(id)) {
            throw new ServicioNoEncontradoException(id);
        }
        return servicios.buscar(id)
                .filter(servicio -> servicio.getPrestadorId().equals(prestadorId))
                .orElseThrow(() -> new ServicioNoEncontradoException(id));
    }

    /** Uno de los tres estados exactos; otro valor es 400 con {@code campos.estado}. */
    static EstadoPublicacion estadoPublicacion(String estado) {
        try {
            return EstadoPublicacion.valueOf(estado);
        } catch (IllegalArgumentException e) {
            throw new ContenidoInvalidoException(Map.of("estado", MotivoInvalido.NO_PERMITIDO));
        }
    }
}
