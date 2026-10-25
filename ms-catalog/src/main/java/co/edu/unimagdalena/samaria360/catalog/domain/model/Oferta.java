package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Oferta inicial de un servicio reservable (DEC-130, DEC-136): los días en que opera y, en un tour, las horas de salida.
 * La capacidad no se repite aquí: un tour ofrece {@code aforoMaxGrupo} cupos por salida y un alojamiento,
 * {@code numHabitaciones} por unidad y noche. ms-booking la materializa bajo demanda (DEC-135). Solo existe en Tour y en
 * Alojamiento: Gastronomía y Alquiler no tienen inventario (D2 de «Reservar servicio»).
 *
 * @param diasOperacion de 1 a 7 días, sin repetir, en el orden de la semana
 * @param salidas       en Tour, de 1 a 12 horas sin repetir, en orden; en Alojamiento, vacía
 */
public record Oferta(List<DiaSemana> diasOperacion, List<LocalTime> salidas) {

    public static final int MAXIMO_SALIDAS = 12;

    public Oferta {
        if (diasOperacion == null || diasOperacion.isEmpty()) {
            throw new IllegalArgumentException("La oferta tiene al menos un día de operación");
        }
        Set<DiaSemana> dias = EnumSet.copyOf(diasOperacion);
        if (dias.size() != diasOperacion.size()) {
            throw new IllegalArgumentException("Días de operación repetidos");
        }
        diasOperacion = List.copyOf(dias);
        salidas = salidas == null ? List.of() : salidas.stream().distinct().sorted().toList();
        if (salidas.size() > MAXIMO_SALIDAS) {
            throw new IllegalArgumentException("Una oferta tiene como máximo " + MAXIMO_SALIDAS + " salidas");
        }
        for (LocalTime salida : salidas) {
            if (salida.getSecond() != 0 || salida.getNano() != 0) {
                throw new IllegalArgumentException("Las salidas van en horas y minutos: " + salida);
            }
        }
    }

    /** Cumple la forma de su categoría: un tour tiene salidas y un alojamiento no; las demás no tienen oferta. */
    boolean correspondeA(Categoria categoria) {
        return switch (categoria) {
            case Tour -> !salidas.isEmpty();
            case Alojamiento -> salidas.isEmpty();
            case Gastronomia, AlquilerEquipo -> false;
        };
    }
}
