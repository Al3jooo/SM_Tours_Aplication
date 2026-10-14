package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.OfertaNoDisponibleException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;

/**
 * Detalle público de un servicio (WF-06 y WF-10): con sus unidades si es un alojamiento (D1). Es el endpoint que
 * consulta ms-booking antes de reservar (ADR-007).
 */
public interface ConsultarServicioUseCase {

    /** @throws OfertaNoDisponibleException si el id no es un ObjectId, no existe o el servicio no es visible */
    ServicioComercial consultar(String id);
}
