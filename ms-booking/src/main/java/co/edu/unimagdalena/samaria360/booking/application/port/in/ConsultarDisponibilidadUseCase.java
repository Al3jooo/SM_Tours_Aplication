package co.edu.unimagdalena.samaria360.booking.application.port.in;

import co.edu.unimagdalena.samaria360.booking.domain.model.DisponibilidadServicio;

/** Cupos de un servicio por día (RF11, WF-10). Pública y sin llamar al catálogo. */
public interface ConsultarDisponibilidadUseCase {

    /** Por defecto, desde hoy y 30 días; como máximo 60 días. @param idUnidad opcional (solo alojamiento) */
    DisponibilidadServicio consultar(String idServicio, String idUnidad, String desde, String hasta);
}
