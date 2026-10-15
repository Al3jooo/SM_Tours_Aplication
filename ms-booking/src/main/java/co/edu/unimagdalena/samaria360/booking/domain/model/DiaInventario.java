package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Objects;

/**
 * Clave de una fila de {@code inventario_cupo} (D1, D2): una noche de una unidad de alojamiento ({@code hora} nula)
 * o un horario de un tour ({@code idUnidad} nula).
 */
public record DiaInventario(String idServicio, String idUnidad, LocalDate fecha, LocalTime hora) {

    /** Orden con el que se toman los bloqueos (D3): por fecha y hora, igual para todas las transacciones. */
    public static final Comparator<DiaInventario> ORDEN = Comparator.comparing(DiaInventario::fecha)
            .thenComparing(DiaInventario::hora, Comparator.nullsFirst(Comparator.naturalOrder()));

    public DiaInventario {
        Objects.requireNonNull(idServicio, "idServicio");
        Objects.requireNonNull(fecha, "fecha");
        if ((idUnidad == null) == (hora == null)) {
            throw new IllegalArgumentException("Un día de inventario es de una unidad (alojamiento) o de un horario (tour)");
        }
    }
}
