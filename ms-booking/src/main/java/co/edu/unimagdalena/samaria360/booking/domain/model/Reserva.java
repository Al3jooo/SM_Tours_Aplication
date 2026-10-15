package co.edu.unimagdalena.samaria360.booking.domain.model;

import co.edu.unimagdalena.samaria360.booking.domain.exception.CancelacionFueraDePlazoException;
import co.edu.unimagdalena.samaria360.booking.domain.exception.ReservaNoCancelableException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Reserva de un turista (RF20). Guarda lo que vio al reservar: el precio, el total, el nombre del servicio y el de la
 * unidad no cambian aunque el catálogo cambie después (RF32). Inmutable: cancelar devuelve otra reserva.
 *
 * @param idTurista          {@code sub} del JWT; referencia lógica a auth_db
 * @param idPrestador        dueño del servicio; referencia lógica a auth_db
 * @param idUnidad           solo alojamiento (D1)
 * @param cantidad           habitaciones (alojamiento) o personas (tour)
 * @param claveIdempotencia  {@code Idempotency-Key} con la que se creó (D6)
 * @param hashSolicitud      SHA-256 del comando normalizado
 */
public record Reserva(UUID idReserva, UUID idTurista, UUID idPrestador, String idServicio, String nombreServicio,
                      CategoriaReservable categoria, String idUnidad, String nombreUnidad, EstadoReserva estado,
                      int cantidad, int personas, BigDecimal precioUnitario, UnidadCobro unidadCobro,
                      BigDecimal montoTotal, String moneda, LocalDate fechaEntrada, LocalDate fechaSalida,
                      LocalTime horaInicio, Instant fechaInicio, Instant fechaFin, Instant fechaCreacion,
                      Instant fechaCancelacion, UUID claveIdempotencia, String hashSolicitud) {

    public static final String MONEDA = "COP";
    /** RF44: se puede cancelar hasta 24 horas antes del inicio. */
    public static final Duration PLAZO_CANCELACION = Duration.ofHours(24);

    public Reserva {
        Objects.requireNonNull(idReserva, "idReserva");
        Objects.requireNonNull(idTurista, "idTurista");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(fechaInicio, "fechaInicio");
    }

    /** RF20: confirmación automática. El id se genera aquí, no en la base. */
    public static Reserva confirmar(ReservaPlanificada plan, UUID idTurista, UUID claveIdempotencia,
                                    String hashSolicitud, Instant ahora) {
        return new Reserva(UUID.randomUUID(), idTurista, plan.idPrestador(), plan.idServicio(), plan.nombreServicio(),
                plan.categoria(), plan.idUnidad(), plan.nombreUnidad(), EstadoReserva.Confirmada, plan.cantidad(),
                plan.personas(), plan.precioUnitario(), plan.unidadCobro(), plan.montoTotal(), MONEDA,
                plan.fechaEntrada(), plan.fechaSalida(), plan.horaInicio(), plan.fechaInicio(), plan.fechaFin(), ahora,
                null, claveIdempotencia, hashSolicitud);
    }

    /** Último instante en que se puede cancelar (inclusivo): el inicio menos 24 horas. */
    public Instant cancelableHasta() {
        return fechaInicio.minus(PLAZO_CANCELACION);
    }

    /**
     * RF44 y estado. Con exactamente 24 horas de anticipación sí se puede; un segundo después, no. Una reserva ya
     * Cancelada no llega aquí: cancelarla otra vez responde lo mismo sin liberar nada (D6).
     */
    public void verificarCancelable(Instant ahora) {
        if (estado == EstadoReserva.Completada) {
            throw new ReservaNoCancelableException();
        }
        if (ahora.isAfter(cancelableHasta())) {
            throw new CancelacionFueraDePlazoException();
        }
    }

    public Reserva cancelada(Instant ahora) {
        return new Reserva(idReserva, idTurista, idPrestador, idServicio, nombreServicio, categoria, idUnidad,
                nombreUnidad, EstadoReserva.Cancelada, cantidad, personas, precioUnitario, unidadCobro, montoTotal,
                moneda, fechaEntrada, fechaSalida, horaInicio, fechaInicio, fechaFin, fechaCreacion, ahora,
                claveIdempotencia, hashSolicitud);
    }
}
