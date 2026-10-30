package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.booking.domain.model.Reserva;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Una reserva de un servicio del Prestador, tal como quedó guardada (RF12, RF32; DEC-192). <b>Sin datos del
 * Turista</b>: ni su id ni su nombre o contacto (Ley 1581; RF42 descartado). Sin {@code cancelableHasta}: el
 * Prestador no cancela.
 */
public record RespuestaReservaPrestador(UUID idReserva, String estado, String idServicio, String nombreServicio,
                                        String categoria, String idUnidad, String nombreUnidad, LocalDate fechaEntrada,
                                        LocalDate fechaSalida, LocalTime horaInicio, Instant fechaInicio,
                                        Instant fechaFin, int cantidad, int personas, BigDecimal precioUnitario,
                                        String unidadCobro, BigDecimal precioTotal, String moneda,
                                        Instant fechaCreacion, Instant fechaCancelacion) {

    public static RespuestaReservaPrestador de(Reserva r) {
        return new RespuestaReservaPrestador(r.idReserva(), r.estado().name(), r.idServicio(), r.nombreServicio(),
                r.categoria().name(), r.idUnidad(), r.nombreUnidad(), r.fechaEntrada(), r.fechaSalida(),
                r.horaInicio(), r.fechaInicio(), r.fechaFin(), r.cantidad(), r.personas(), r.precioUnitario(),
                r.unidadCobro().name(), r.montoTotal(), r.moneda(), r.fechaCreacion(), r.fechaCancelacion());
    }
}
