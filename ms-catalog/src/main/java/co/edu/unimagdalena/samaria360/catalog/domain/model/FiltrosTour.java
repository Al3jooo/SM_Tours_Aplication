package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @param nivelDificultad  nivel de dificultad exacto; los tours sin nivel no coinciden
 * @param duracionMaxHoras duración máxima en horas (mayor que 0)
 */
public record FiltrosTour(String nivelDificultad, BigDecimal duracionMaxHoras) implements FiltrosCategoria {

    public FiltrosTour {
        nivelDificultad = TextoFiltro.normalizar(nivelDificultad, "nivelDificultad");
        if (duracionMaxHoras != null && duracionMaxHoras.signum() <= 0) {
            throw new CriteriosBusquedaInvalidosException("duracionMaxHoras debe ser mayor que 0");
        }
    }

    @Override
    public Categoria categoria() {
        return Categoria.Tour;
    }

    @Override
    public List<String> camposActivos() {
        List<String> campos = new ArrayList<>();
        if (nivelDificultad != null) {
            campos.add("nivelDificultad");
        }
        if (duracionMaxHoras != null) {
            campos.add("duracionMaxHoras");
        }
        return List.copyOf(campos);
    }

    @Override
    public boolean cumple(DetalleCategoria detalle) {
        return detalle instanceof DetalleTour tour
                && TextoFiltro.coincide(nivelDificultad, tour.nivelDificultad())
                && (duracionMaxHoras == null || tour.duracionHoras().compareTo(duracionMaxHoras) <= 0);
    }
}
