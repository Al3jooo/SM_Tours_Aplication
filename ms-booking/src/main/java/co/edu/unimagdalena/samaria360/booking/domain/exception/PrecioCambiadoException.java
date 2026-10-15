package co.edu.unimagdalena.samaria360.booking.domain.exception;

import java.math.BigDecimal;

/**
 * El precio que vio el turista ({@code precioTotalEsperado}) ya no es el del catálogo (RF37, 409
 * {@code PRECIO_CAMBIADO}). Lleva el precio nuevo para mostrarlo; no se reserva nada.
 */
public class PrecioCambiadoException extends RuntimeException {

    private final BigDecimal precioTotal;
    private final BigDecimal precioUnitario;

    public PrecioCambiadoException(BigDecimal precioTotal, BigDecimal precioUnitario) {
        super("El precio cambió: el total vigente es " + precioTotal.toPlainString());
        this.precioTotal = precioTotal;
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getPrecioTotal() {
        return precioTotal;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }
}
