package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * «Más filtros» de Alojamiento. Los servicios incluidos son del alojamiento; los huéspedes y el tipo de habitación
 * son de las unidades (D1): el alojamiento cumple si <b>una misma unidad</b> cumple todos los filtros de unidad
 * activos, junto con el precio máximo de la búsqueda, porque una reserva es de una sola unidad. Por ejemplo, con
 * «Doble: 2 huéspedes a 150.000» y «Suite: 6 a 260.000», «4 huéspedes y hasta 200.000» no lo encuentra.
 *
 * @param serviciosIncluidos el alojamiento debe incluirlos todos
 * @param huespedes          número de huéspedes: la capacidad de la unidad debe alcanzarlo (mayor que 0)
 * @param tipoHabitacion     tipo de habitación exacto de la unidad
 */
public record FiltrosAlojamiento(Set<ServicioIncluido> serviciosIncluidos, Integer huespedes, String tipoHabitacion)
        implements FiltrosCategoria {

    public FiltrosAlojamiento {
        serviciosIncluidos = serviciosIncluidos == null ? Set.of() : Set.copyOf(serviciosIncluidos);
        if (huespedes != null && huespedes <= 0) {
            throw new CriteriosBusquedaInvalidosException("huespedes debe ser mayor que 0");
        }
        tipoHabitacion = TextoFiltro.normalizar(tipoHabitacion, "tipoHabitacion");
    }

    /** Nombres en el orden del vocabulario (WiFi, Desayuno, Piscina, Parqueadero). */
    public List<String> nombresServiciosIncluidos() {
        return serviciosIncluidos.stream().sorted().map(Enum::name).toList();
    }

    @Override
    public Categoria categoria() {
        return Categoria.Alojamiento;
    }

    @Override
    public List<String> camposActivos() {
        List<String> campos = new ArrayList<>();
        if (!serviciosIncluidos.isEmpty()) {
            campos.add("serviciosIncluidos");
        }
        if (huespedes != null) {
            campos.add("huespedes");
        }
        if (tipoHabitacion != null) {
            campos.add("tipoHabitacion");
        }
        return List.copyOf(campos);
    }

    /** Si hay filtros que se evalúan sobre las unidades (huéspedes o tipo de habitación). */
    public boolean filtraUnidades() {
        return huespedes != null || tipoHabitacion != null;
    }

    @Override
    public boolean cumple(DetalleCategoria detalle, BigDecimal precioMaximo) {
        return detalle instanceof DetalleAlojamiento alojamiento
                && alojamiento.serviciosIncluidos().containsAll(nombresServiciosIncluidos())
                && (!filtraUnidades()
                || alojamiento.unidades().stream().anyMatch(unidad -> cumple(unidad, precioMaximo)));
    }

    /**
     * Precio «desde» de la tarjeta con filtros de unidad activos: el menor precio por noche de las unidades que cumplen
     * los filtros de unidad y el precio máximo; null si ninguna cumple. Sin filtros de unidad, el del servicio.
     */
    public BigDecimal precioDesde(DetalleAlojamiento alojamiento, BigDecimal precioMaximo) {
        if (!filtraUnidades()) {
            return alojamiento.precioDesde();
        }
        return alojamiento.unidades().stream().filter(unidad -> cumple(unidad, precioMaximo))
                .map(UnidadAlojamiento::precioNoche).min(Comparator.naturalOrder()).orElse(null);
    }

    private boolean cumple(UnidadAlojamiento unidad, BigDecimal precioMaximo) {
        return (huespedes == null || unidad.capacidadMaxHuespedes() >= huespedes)
                && TextoFiltro.coincide(tipoHabitacion, unidad.tipoHabitacion())
                && (precioMaximo == null || unidad.precioNoche().compareTo(precioMaximo) <= 0);
    }
}
