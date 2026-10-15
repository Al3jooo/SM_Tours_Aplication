package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.util.List;

/**
 * Cupos de un servicio en un rango de días (RF11, WF-10): por unidad y noche en un alojamiento, por horario en un
 * tour. No dice nombres ni precios: los da el detalle de ms-catalog (ADR-007).
 */
public record DisponibilidadServicio(String idServicio, RangoFechas rango, List<CupoDia> cupos) {

    public DisponibilidadServicio {
        cupos = List.copyOf(cupos);
    }
}
