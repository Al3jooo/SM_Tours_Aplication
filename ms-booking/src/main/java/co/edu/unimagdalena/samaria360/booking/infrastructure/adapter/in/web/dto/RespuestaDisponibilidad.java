package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.CupoDia;
import co.edu.unimagdalena.samaria360.booking.domain.model.DisponibilidadServicio;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cupos de un servicio (RF11, WF-10). En un alojamiento, agrupados por unidad (D1); en un tour, por horario. Sin
 * nombres ni precios: el cliente los toma del detalle de ms-catalog (ADR-007).
 *
 * @param categoria             null si no hay inventario en el rango
 * @param horaCheckIn           hora de entrada de los alojamientos (configuración de ms-booking)
 * @param horaCheckOut          hora de salida de los alojamientos
 * @param plazoCancelacionHoras RF44: se puede cancelar hasta estas horas antes del inicio
 */
public record RespuestaDisponibilidad(String idServicio, LocalDate desde, LocalDate hasta, String categoria,
                                      List<Unidad> unidades, List<Horario> horarios, LocalTime horaCheckIn,
                                      LocalTime horaCheckOut, long plazoCancelacionHoras) {

    public record Unidad(String idUnidad, List<Noche> noches) {
    }

    public record Noche(LocalDate fecha, int cuposTotales, int cuposDisponibles) {
    }

    public record Horario(LocalDate fecha, LocalTime hora, int cuposTotales, int cuposDisponibles) {
    }

    public static RespuestaDisponibilidad de(DisponibilidadServicio disponibilidad) {
        Map<String, List<Noche>> porUnidad = new LinkedHashMap<>();
        List<Horario> horarios = new ArrayList<>();
        String categoria = null;
        for (CupoDia cupo : disponibilidad.cupos()) {
            categoria = cupo.categoria().name();
            if (cupo.categoria() == CategoriaReservable.Alojamiento) {
                porUnidad.computeIfAbsent(cupo.idUnidad(), id -> new ArrayList<>())
                        .add(new Noche(cupo.fecha(), cupo.cuposTotales(), cupo.cuposDisponibles()));
            } else {
                horarios.add(new Horario(cupo.fecha(), cupo.hora(), cupo.cuposTotales(), cupo.cuposDisponibles()));
            }
        }
        List<Unidad> unidades = porUnidad.entrySet().stream()
                .map(entrada -> new Unidad(entrada.getKey(), entrada.getValue())).toList();
        return new RespuestaDisponibilidad(disponibilidad.idServicio(), disponibilidad.rango().desde(),
                disponibilidad.rango().hasta(), categoria, unidades, horarios, disponibilidad.horario().checkIn(),
                disponibilidad.horario().checkOut(), disponibilidad.plazoCancelacion().toHours());
    }
}
