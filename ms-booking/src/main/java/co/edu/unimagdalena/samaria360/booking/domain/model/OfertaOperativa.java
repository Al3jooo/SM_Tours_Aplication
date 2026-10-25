package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Oferta que el catálogo define para un servicio reservable (DEC-135, DEC-136): los días de la semana en que opera y,
 * en un tour, las horas de salida. La capacidad no está aquí: sale de {@code aforoMaxGrupo} y de cada unidad.
 *
 * @param dias    de 1 a 7 días de la semana
 * @param salidas en un tour, las horas de salida en orden y sin repetir; vacía en un alojamiento
 */
public record OfertaOperativa(Set<DayOfWeek> dias, List<LocalTime> salidas) {

    public OfertaOperativa {
        Objects.requireNonNull(dias, "dias");
        if (dias.isEmpty()) {
            throw new IllegalArgumentException("La oferta opera al menos un día de la semana");
        }
        dias = Collections.unmodifiableSet(EnumSet.copyOf(dias));
        salidas = salidas == null ? List.of() : salidas.stream().distinct().sorted().toList();
    }

    /** En un alojamiento, la fecha es la de entrada de la noche (D2). */
    public boolean opera(LocalDate fecha) {
        return dias.contains(fecha.getDayOfWeek());
    }
}
