package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Lo que una reserva descontó de una fila de inventario ({@code reserva_cupo_dia}): se devuelve igual al cancelar,
 * en el mismo orden de fecha y hora que al reservar (D3).
 */
public record CupoDescontado(long idInventario, LocalDate fecha, LocalTime hora, int cantidad) {
}
