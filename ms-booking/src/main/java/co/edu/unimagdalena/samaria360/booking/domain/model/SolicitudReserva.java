package co.edu.unimagdalena.samaria360.booking.domain.model;

import co.edu.unimagdalena.samaria360.booking.domain.exception.DatosReservaInvalidosException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * Lo que pide el turista, con el formato ya validado. Las reglas que dependen de la categoría (fechas de salida,
 * horario, unidad, capacidad) se validan después de consultar el catálogo ({@code PoliticaReserva}).
 *
 * @param idUnidad            solo alojamiento (D1)
 * @param fechaSalida         solo alojamiento: rango semiabierto [entrada, salida)
 * @param horaInicio          solo tour
 * @param cantidad            habitaciones de la unidad (solo alojamiento)
 * @param personas            huéspedes o personas del tour
 * @param precioTotalEsperado el total que vio el turista (RF37): si no coincide, {@code PRECIO_CAMBIADO}
 */
public record SolicitudReserva(String idServicio, String idUnidad, LocalDate fechaEntrada, LocalDate fechaSalida,
                               LocalTime horaInicio, Integer cantidad, int personas, BigDecimal precioTotalEsperado) {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** @throws DatosReservaInvalidosException con todos los campos con formato inválido */
    public static SolicitudReserva validar(String idServicio, String idUnidad, String fechaEntrada, String fechaSalida,
                                           String horaInicio, Integer cantidad, Integer personas,
                                           BigDecimal precioTotalEsperado) {
        ValidadorCampos validador = new ValidadorCampos();
        String servicio = validador.objectId("idServicio", idServicio, true);
        String unidad = validador.objectId("idUnidad", idUnidad, false);
        LocalDate entrada = validador.fecha("fechaEntrada", fechaEntrada, true);
        LocalDate salida = validador.fecha("fechaSalida", fechaSalida, false);
        LocalTime hora = validador.hora("horaInicio", horaInicio);
        if (cantidad != null && cantidad < 1) {
            validador.error("cantidad", MotivoInvalido.VALOR_MINIMO);
        }
        if (personas == null) {
            validador.error("personas", MotivoInvalido.REQUERIDO);
        } else if (personas < 1) {
            validador.error("personas", MotivoInvalido.VALOR_MINIMO);
        }
        if (precioTotalEsperado == null) {
            validador.error("precioTotalEsperado", MotivoInvalido.REQUERIDO);
        } else if (precioTotalEsperado.signum() < 0) {
            validador.error("precioTotalEsperado", MotivoInvalido.VALOR_MINIMO);
        }
        validador.lanzarSiHayErrores();
        return new SolicitudReserva(servicio, unidad, entrada, salida, hora == null ? null : hora.withSecond(0).withNano(0),
                cantidad, personas, precioTotalEsperado);
    }

    /**
     * SHA-256 del comando normalizado (D6): el mismo pedido escrito de otra forma (espacios, «08:00:00» frente a
     * «08:00», «150000.00» frente a «150000») da el mismo hash; cualquier dato distinto, incluida la unidad, otro.
     */
    public String hash() {
        String normalizado = String.join("|",
                "idServicio=" + idServicio,
                "idUnidad=" + (idUnidad == null ? "" : idUnidad),
                "fechaEntrada=" + fechaEntrada,
                "fechaSalida=" + (fechaSalida == null ? "" : fechaSalida),
                "horaInicio=" + (horaInicio == null ? "" : HORA.format(horaInicio)),
                "cantidad=" + (cantidad == null ? "" : cantidad),
                "personas=" + personas,
                "precioTotalEsperado=" + precioTotalEsperado.stripTrailingZeros().toPlainString());
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(normalizado.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
