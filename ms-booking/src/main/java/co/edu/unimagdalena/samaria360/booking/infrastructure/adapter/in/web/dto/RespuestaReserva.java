package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.booking.domain.model.Reserva;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Una reserva tal como quedó guardada (RF32): el precio, el nombre del servicio y el de la unidad no cambian aunque
 * cambie el catálogo. No incluye la clave de idempotencia ni el hash.
 *
 * @param cancelableHasta último instante en que se puede cancelar (RF44): el inicio menos 24 horas
 */
public record RespuestaReserva(UUID idReserva, String estado, String idServicio, String nombreServicio,
                               String categoria, String idUnidad, String nombreUnidad, LocalDate fechaEntrada,
                               LocalDate fechaSalida, LocalTime horaInicio, Instant fechaInicio, Instant fechaFin,
                               int cantidad, int personas, BigDecimal precioUnitario, String unidadCobro,
                               BigDecimal precioTotal, String moneda, Instant fechaCreacion,
                               Instant fechaCancelacion, Instant cancelableHasta) {

    public static RespuestaReserva de(Reserva r) {
        return new RespuestaReserva(r.idReserva(), r.estado().name(), r.idServicio(), r.nombreServicio(),
                r.categoria().name(), r.idUnidad(), r.nombreUnidad(), r.fechaEntrada(), r.fechaSalida(),
                r.horaInicio(), r.fechaInicio(), r.fechaFin(), r.cantidad(), r.personas(), r.precioUnitario(),
                r.unidadCobro().name(), r.montoTotal(), r.moneda(), r.fechaCreacion(), r.fechaCancelacion(),
                r.cancelableHasta());
    }
}
