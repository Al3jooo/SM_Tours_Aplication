package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Una solicitud ya contrastada con el catálogo: los datos que se congelan en la reserva (RF32) y los días de inventario
 * que hay que descontar, ordenados para tomar los bloqueos siempre en el mismo orden (D3).
 *
 * @param cantidad           habitaciones (alojamiento) o personas (tour): lo que se descuenta de cada día
 * @param diasRequeridos     en {@link DiaInventario#ORDEN}
 */
public record ReservaPlanificada(String idServicio, UUID idPrestador, String nombreServicio,
                                 CategoriaReservable categoria, String idUnidad, String nombreUnidad, int cantidad,
                                 int personas, BigDecimal precioUnitario, UnidadCobro unidadCobro,
                                 BigDecimal montoTotal, LocalDate fechaEntrada, LocalDate fechaSalida,
                                 LocalTime horaInicio, Instant fechaInicio, Instant fechaFin,
                                 List<DiaInventario> diasRequeridos) {

    public ReservaPlanificada {
        diasRequeridos = diasRequeridos.stream().sorted(DiaInventario.ORDEN).toList();
    }
}
