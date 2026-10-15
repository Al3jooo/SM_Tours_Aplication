package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Convenciones de fecha y hora de las reservas (§5 del plan). Todo se interpreta en la hora de Santa Marta; Colombia no
 * tiene horario de verano, pero se usa {@link ZoneId} igual.
 *
 * @param checkIn  hora de entrada al alojamiento (15:00 por defecto); es la «fecha programada» de RF44
 * @param checkOut hora de salida del alojamiento (12:00 por defecto)
 */
public record HorarioReservas(LocalTime checkIn, LocalTime checkOut) {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");
    /** Una transacción bloquea como mucho 14 filas de inventario. */
    public static final int MAXIMO_NOCHES = 14;
    public static final HorarioReservas POR_DEFECTO = new HorarioReservas(LocalTime.of(15, 0), LocalTime.of(12, 0));

    public HorarioReservas {
        Objects.requireNonNull(checkIn, "checkIn");
        Objects.requireNonNull(checkOut, "checkOut");
    }

    public static LocalDate hoy(Instant ahora) {
        return ahora.atZone(ZONA).toLocalDate();
    }

    public static Instant instante(LocalDate fecha, LocalTime hora) {
        return fecha.atTime(hora).atZone(ZONA).toInstant();
    }
}
