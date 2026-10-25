package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.EstadoServicioCambiadoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ServicioNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;

import java.util.Optional;
import java.util.UUID;

/**
 * CU «Aprobar o rechazar servicio» (RF21) con su «include» «Consultar listado pendiente» y su «extend» «Justificar
 * rechazo de servicio» (DEC-128, DEC-144, DEC-145). Solo el Administrador (lo exige la infraestructura).
 */
public interface AdministrarServiciosUseCase {

    int TAMANO_PAGINA = 10;

    /** Un servicio para revisar, con el estado de la cuenta del prestador según la proyección (si existe). */
    record ServicioEnRevision(ServicioComercial servicio, Optional<EstadoCuenta> estadoCuentaPrestador) {
    }

    /** @param estado nulo o vacío es {@code Pendiente}: la cola de revisión, del más antiguo al más reciente */
    PaginaResultados<ServicioComercial> listar(String estado, int pagina);

    /** Sin la regla de visibilidad pública. @throws ServicioNoEncontradoException */
    ServicioEnRevision detalle(String id);

    /**
     * Pendiente → Publicado ({@code Aprobar}) o Rechazado ({@code Rechazar}, con justificación de 1 a 500 caracteres).
     *
     * @throws ContenidoInvalidoException      si la decisión o la justificación no sirven
     * @throws ServicioNoEncontradoException   si no existe o el id no es válido
     * @throws EstadoServicioCambiadoException si ya no estaba Pendiente
     */
    ServicioEnRevision decidir(String id, String decision, String justificacion, UUID administrador);
}
