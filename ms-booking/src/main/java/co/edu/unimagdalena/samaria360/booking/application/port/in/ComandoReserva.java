package co.edu.unimagdalena.samaria360.booking.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Pedido de reserva tal como llega: los textos se validan en el dominio para responder un código por campo.
 *
 * @param idTurista         {@code sub} del JWT
 * @param claveIdempotencia {@code Idempotency-Key} (D6)
 */
public record ComandoReserva(UUID idTurista, UUID claveIdempotencia, String idServicio, String idUnidad,
                             String fechaEntrada, String fechaSalida, String horaInicio, Integer cantidad,
                             Integer personas, BigDecimal precioTotalEsperado) {
}
