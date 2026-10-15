package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Cupos de una noche de una unidad (alojamiento) o de un horario (tour), tal como están en {@code inventario_cupo}.
 *
 * @param idUnidad solo alojamiento
 * @param hora     solo tour
 */
public record CupoDia(CategoriaReservable categoria, String idUnidad, LocalDate fecha, LocalTime hora,
                      int cuposTotales, int cuposDisponibles) {
}
