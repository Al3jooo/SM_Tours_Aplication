package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Rango de días [desde, hasta], ambos incluidos, de una consulta de disponibilidad.
 */
public record RangoFechas(LocalDate desde, LocalDate hasta) {

    /**
     * Tope del largo de una consulta de disponibilidad (ajuste aprobado 7). Coincide con el horizonte de materialización
     * ({@code MaterializacionOferta.DIAS_HORIZONTE}, DEC-153), pero son reglas distintas: este limita el rango pedido.
     */
    public static final int MAXIMO_DIAS = 60;

    /**
     * Sin {@code desde}, hoy; sin {@code hasta}, {@code desde} más {@code diasPorDefecto} − 1. Errores en los campos
     * {@code desde} y {@code hasta}: FORMATO, RANGO_INVALIDO (hasta antes de desde) o MAXIMO_DIAS.
     */
    public static RangoFechas validar(String desde, String hasta, LocalDate hoy, int diasPorDefecto,
                                      ValidadorCampos validador) {
        LocalDate inicio = validador.fecha("desde", desde, false);
        LocalDate fin = validador.fecha("hasta", hasta, false);
        if (validador.tieneError("desde") || validador.tieneError("hasta")) {
            return null;
        }
        inicio = inicio == null ? hoy : inicio;
        fin = fin == null ? inicio.plusDays(diasPorDefecto - 1L) : fin;
        if (fin.isBefore(inicio)) {
            validador.error("hasta", MotivoInvalido.RANGO_INVALIDO);
            return null;
        }
        if (ChronoUnit.DAYS.between(inicio, fin) + 1 > MAXIMO_DIAS) {
            validador.error("hasta", MotivoInvalido.MAXIMO_DIAS);
            return null;
        }
        return new RangoFechas(inicio, fin);
    }
}
