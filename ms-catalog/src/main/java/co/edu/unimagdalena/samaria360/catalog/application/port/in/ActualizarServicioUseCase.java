package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.EstadoServicioCambiadoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ServicioModificadoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ServicioNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ActualizacionPrecio.PrecioUnidad;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosContacto;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Edición de un servicio por su Prestador: CU «Actualizar tarifas» (RF32) y la parte del servicio de «Actualizar datos
 * comerciales» (RF43). Solo los del claim {@code sub}, en estado Pendiente o Publicado, sin nueva aprobación (DEC-185,
 * DEC-189). Cada edición compara la versión leída y la sube (DEC-186). Las dos operaciones lanzan:
 * {@link ServicioNoEncontradoException} si no existe, el id no es válido o es de otro prestador (404);
 * {@link EstadoServicioCambiadoException} si está Rechazado (409 ESTADO_CAMBIADO);
 * {@link ServicioModificadoException} si la versión ya no es la leída (409 SERVICIO_MODIFICADO), y
 * {@link ContenidoInvalidoException} con un motivo por cada campo inválido (400).
 */
public interface ActualizarServicioUseCase {

    /** @param precioBase se ignora en Alojamiento; {@code unidades} se ignora en las demás categorías */
    ServicioComercial actualizarPrecio(UUID prestadorId, String id, BigDecimal precioBase, List<PrecioUnidad> unidades,
                                       Integer version);

    ServicioComercial actualizarDatosComerciales(UUID prestadorId, String id, String nombreComercial,
                                                 DatosContacto contacto, Integer version);
}
