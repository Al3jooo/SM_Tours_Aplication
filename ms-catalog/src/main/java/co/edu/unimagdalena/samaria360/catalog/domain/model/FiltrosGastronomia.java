package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @param soloVegetariano solo los que ofrecen menú vegetariano (RF10 «menú vegetariano/vegano»; el esquema lo guarda
 *                        como {@code menuVegetariano}). {@code false} equivale a no filtrar
 * @param tipoCocina      tipo de cocina exacto
 */
public record FiltrosGastronomia(boolean soloVegetariano, String tipoCocina) implements FiltrosCategoria {

    public FiltrosGastronomia {
        tipoCocina = TextoFiltro.normalizar(tipoCocina, "tipoCocina");
    }

    @Override
    public Categoria categoria() {
        return Categoria.Gastronomia;
    }

    @Override
    public List<String> camposActivos() {
        List<String> campos = new ArrayList<>();
        if (soloVegetariano) {
            campos.add("menuVegetariano");
        }
        if (tipoCocina != null) {
            campos.add("tipoCocina");
        }
        return List.copyOf(campos);
    }

    @Override
    public boolean cumple(DetalleCategoria detalle) {
        return detalle instanceof DetalleGastronomia gastronomia
                && (!soloVegetariano || Boolean.TRUE.equals(gastronomia.menuVegetariano()))
                && TextoFiltro.coincide(tipoCocina, gastronomia.tipoCocina());
    }
}
