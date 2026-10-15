package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;

/**
 * Cuerpo de {@code POST /api/v1/reservas}. Las fechas y la hora van como texto ISO para responder un código por campo
 * (FORMATO) en lugar de un error de todo el cuerpo.
 *
 * @param idUnidad            solo alojamiento (D1)
 * @param fechaSalida         solo alojamiento
 * @param horaInicio          solo tour (hh:mm)
 * @param cantidad            habitaciones (solo alojamiento)
 * @param precioTotalEsperado el total que ve el turista (RF37)
 */
public record SolicitudReservaDto(String idServicio, String idUnidad, String fechaEntrada, String fechaSalida,
                                  String horaInicio, Integer cantidad, Integer personas,
                                  BigDecimal precioTotalEsperado) {
}
