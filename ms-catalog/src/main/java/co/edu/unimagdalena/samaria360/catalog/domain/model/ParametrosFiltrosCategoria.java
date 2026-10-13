package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parámetros de «Más filtros» tal como llegan en la consulta, antes de saber a qué categoría pertenecen. Todos son
 * opcionales; {@code menuVegetariano=false} equivale a no filtrar.
 */
public record ParametrosFiltrosCategoria(List<String> serviciosIncluidos, Integer huespedes, String tipoHabitacion,
                                         Boolean menuVegetariano, String tipoCocina, String nivelDificultad,
                                         BigDecimal duracionMaxHoras, String tipoVehiculoEquipo) {

    public ParametrosFiltrosCategoria {
        serviciosIncluidos = serviciosIncluidos == null ? List.of()
                : serviciosIncluidos.stream().filter(Objects::nonNull).filter(valor -> !valor.isBlank()).toList();
    }

    public static ParametrosFiltrosCategoria ninguno() {
        return new ParametrosFiltrosCategoria(null, null, null, null, null, null, null, null);
    }

    /**
     * Filtros de la categoría de la búsqueda, o null si no llegó ninguno. Cada parámetro exige su categoría: si falta
     * o es otra, el mensaje dice cuál (p. ej. «tipoCocina requiere categoria=Gastronomia»).
     */
    public FiltrosCategoria paraCategoria(Categoria categoria) {
        Map<String, Categoria> presentes = presentes();
        presentes.forEach((parametro, suCategoria) -> {
            if (suCategoria != categoria) {
                throw new CriteriosBusquedaInvalidosException(parametro + " requiere categoria=" + suCategoria.name());
            }
        });
        if (presentes.isEmpty()) {
            return null;
        }
        return switch (categoria) {
            case Alojamiento -> new FiltrosAlojamiento(servicios(), huespedes, tipoHabitacion);
            case Gastronomia -> new FiltrosGastronomia(Boolean.TRUE.equals(menuVegetariano), tipoCocina);
            case Tour -> new FiltrosTour(nivelDificultad, duracionMaxHoras);
            case AlquilerEquipo -> new FiltrosAlquilerEquipo(tipoVehiculoEquipo);
        };
    }

    /** Parámetros con valor y la categoría que exige cada uno, en el orden de la API. */
    private Map<String, Categoria> presentes() {
        Map<String, Categoria> presentes = new LinkedHashMap<>();
        if (!serviciosIncluidos.isEmpty()) {
            presentes.put("serviciosIncluidos", Categoria.Alojamiento);
        }
        if (huespedes != null) {
            presentes.put("huespedes", Categoria.Alojamiento);
        }
        if (conTexto(tipoHabitacion)) {
            presentes.put("tipoHabitacion", Categoria.Alojamiento);
        }
        if (Boolean.TRUE.equals(menuVegetariano)) {
            presentes.put("menuVegetariano", Categoria.Gastronomia);
        }
        if (conTexto(tipoCocina)) {
            presentes.put("tipoCocina", Categoria.Gastronomia);
        }
        if (conTexto(nivelDificultad)) {
            presentes.put("nivelDificultad", Categoria.Tour);
        }
        if (duracionMaxHoras != null) {
            presentes.put("duracionMaxHoras", Categoria.Tour);
        }
        if (conTexto(tipoVehiculoEquipo)) {
            presentes.put("tipoVehiculoEquipo", Categoria.AlquilerEquipo);
        }
        return presentes;
    }

    /** Un valor desconocido es un criterio inválido; el mensaje lista los aceptados y recorta el recibido. */
    private Set<ServicioIncluido> servicios() {
        try {
            return serviciosIncluidos.stream().map(ServicioIncluido::desdeTexto).collect(Collectors.toSet());
        } catch (IllegalArgumentException e) {
            throw new CriteriosBusquedaInvalidosException(e.getMessage());
        }
    }

    private static boolean conTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
