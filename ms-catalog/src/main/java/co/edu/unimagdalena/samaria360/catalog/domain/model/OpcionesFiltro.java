package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Opciones de «Más filtros» de una categoría (WF-05, RF10): los valores que tienen hoy los servicios visibles en la
 * búsqueda pública (publicados y de prestador activo). Así, cada opción ofrecida devuelve al menos un resultado.
 * Solo trae los campos de su categoría; los demás quedan en null. Las listas van ordenadas en español.
 *
 * @param huespedes         capacidad máxima de huéspedes, de la menor a la mayor
 * @param opcionVegetariana si algún restaurante ofrece menú vegetariano
 * @param duracionHoras     duración de los tours, de la menor a la mayor
 */
public record OpcionesFiltro(Categoria categoria, List<ServicioIncluido> serviciosIncluidos,
                             List<String> tiposHabitacion, Rango<Integer> huespedes, Boolean opcionVegetariana,
                             List<String> tiposCocina, List<String> nivelesDificultad, Rango<BigDecimal> duracionHoras,
                             List<String> tiposVehiculoEquipo) {

    /** Mínimo y máximo de un campo numérico; null si no hay servicios visibles. */
    public record Rango<T extends Comparable<? super T>>(T minimo, T maximo) {

        public Rango {
            Objects.requireNonNull(minimo, "minimo");
            Objects.requireNonNull(maximo, "maximo");
            if (minimo.compareTo(maximo) > 0) {
                throw new IllegalArgumentException("minimo no puede ser mayor que maximo");
            }
        }
    }

    public OpcionesFiltro {
        Objects.requireNonNull(categoria, "categoria");
        serviciosIncluidos = serviciosIncluidos == null ? null
                : serviciosIncluidos.stream().distinct().sorted().toList();
        tiposHabitacion = ordenada(tiposHabitacion);
        tiposCocina = ordenada(tiposCocina);
        nivelesDificultad = ordenada(nivelesDificultad);
        tiposVehiculoEquipo = ordenada(tiposVehiculoEquipo);
    }

    public static OpcionesFiltro alojamiento(Collection<ServicioIncluido> serviciosIncluidos,
                                             Collection<String> tiposHabitacion, Rango<Integer> huespedes) {
        return new OpcionesFiltro(Categoria.Alojamiento, List.copyOf(serviciosIncluidos), List.copyOf(tiposHabitacion),
                huespedes, null, null, null, null, null);
    }

    public static OpcionesFiltro gastronomia(boolean opcionVegetariana, Collection<String> tiposCocina) {
        return new OpcionesFiltro(Categoria.Gastronomia, null, null, null, opcionVegetariana,
                List.copyOf(tiposCocina), null, null, null);
    }

    public static OpcionesFiltro tour(Collection<String> nivelesDificultad, Rango<BigDecimal> duracionHoras) {
        return new OpcionesFiltro(Categoria.Tour, null, null, null, null, null, List.copyOf(nivelesDificultad),
                duracionHoras, null);
    }

    public static OpcionesFiltro alquilerEquipo(Collection<String> tiposVehiculoEquipo) {
        return new OpcionesFiltro(Categoria.AlquilerEquipo, null, null, null, null, null, null, null,
                List.copyOf(tiposVehiculoEquipo));
    }

    /** Orden alfabético en español (la «ñ» después de la «n»; las tildes no alteran el orden). Sin repetidos. */
    private static List<String> ordenada(List<String> valores) {
        if (valores == null) {
            return null;
        }
        Collator collator = Collator.getInstance(Locale.forLanguageTag("es"));
        return valores.stream().filter(Objects::nonNull).distinct().sorted(collator).toList();
    }
}
