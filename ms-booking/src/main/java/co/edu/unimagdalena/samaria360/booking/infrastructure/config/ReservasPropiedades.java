package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;

/**
 * Horario de los alojamientos (samaria360.reservas.*, §5 del plan): 15:00 y 12:00 por defecto.
 */
@ConfigurationProperties("samaria360.reservas")
public record ReservasPropiedades(LocalTime horaCheckIn, LocalTime horaCheckOut) {
}
