package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.math.BigDecimal;

/**
 * Unidad de un alojamiento tal como la publica ms-catalog (D1).
 *
 * @param numHabitaciones       habitaciones de este tipo: los cupos de cada noche que crea la materialización (DEC-135)
 * @param capacidadMaxHuespedes huéspedes por habitación
 * @param precioNoche           por noche y por habitación, en COP
 */
public record UnidadReservable(String idUnidad, String nombre, int numHabitaciones, int capacidadMaxHuespedes,
                               BigDecimal precioNoche) {
}
