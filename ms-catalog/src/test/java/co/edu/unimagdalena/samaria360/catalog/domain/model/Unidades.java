package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Unidades de alojamiento para las pruebas (D1), con un {@code idUnidad} distinto en cada una. */
public final class Unidades {

    private static final AtomicLong SECUENCIA = new AtomicLong(1);

    private Unidades() {
    }

    /** Unidad cuyo nombre es su tipo de habitación, como las que deja la migración M001. */
    public static UnidadAlojamiento unidad(String tipoHabitacion, int numHabitaciones, int capacidadMaxHuespedes,
                                           String precioNoche) {
        return unidad(tipoHabitacion, tipoHabitacion, numHabitaciones, capacidadMaxHuespedes, precioNoche);
    }

    public static UnidadAlojamiento unidad(String nombre, String tipoHabitacion, int numHabitaciones,
                                           int capacidadMaxHuespedes, String precioNoche) {
        return new UnidadAlojamiento(nuevoId(), nombre, tipoHabitacion, numHabitaciones, capacidadMaxHuespedes,
                new BigDecimal(precioNoche));
    }

    /** Alojamiento de una sola unidad. */
    public static DetalleAlojamiento alojamiento(String tipoHabitacion, int numHabitaciones, int capacidadMaxHuespedes,
                                                 String precioNoche, List<String> serviciosIncluidos) {
        return new DetalleAlojamiento(
                List.of(unidad(tipoHabitacion, numHabitaciones, capacidadMaxHuespedes, precioNoche)),
                serviciosIncluidos);
    }

    /** Texto con la forma de un ObjectId (24 hexadecimales en minúscula). */
    public static String nuevoId() {
        return String.format("%024x", SECUENCIA.getAndIncrement());
    }
}
