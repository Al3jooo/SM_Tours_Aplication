package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @param serviciosIncluidos el alojamiento debe incluirlos todos
 * @param huespedes          número de huéspedes: la capacidad máxima debe alcanzarlo (mayor que 0)
 * @param tipoHabitacion     tipo de habitación exacto
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

    @Override
    public boolean cumple(DetalleCategoria detalle) {
        return detalle instanceof DetalleAlojamiento alojamiento
                && alojamiento.serviciosIncluidos().containsAll(nombresServiciosIncluidos())
                && (huespedes == null || alojamiento.capacidadMaxHuespedes() >= huespedes)
                && TextoFiltro.coincide(tipoHabitacion, alojamiento.tipoHabitacion());
    }
}
