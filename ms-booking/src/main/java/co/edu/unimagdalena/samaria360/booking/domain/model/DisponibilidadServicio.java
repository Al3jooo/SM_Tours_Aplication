package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.time.Duration;
import java.util.List;

/**
 * Cupos de un servicio en un rango de días (RF11, WF-10): por unidad y noche en un alojamiento, por horario en un
 * tour. No dice nombres ni precios: los da el detalle de ms-catalog (ADR-007). Lleva el horario de los alojamientos
 * y el plazo de cancelación (RF44) que aplica ms-booking, para que la pantalla no los escriba por su cuenta.
 */
public record DisponibilidadServicio(String idServicio, RangoFechas rango, List<CupoDia> cupos,
                                     HorarioReservas horario, Duration plazoCancelacion) {

    public DisponibilidadServicio {
        cupos = List.copyOf(cupos);
    }
}
