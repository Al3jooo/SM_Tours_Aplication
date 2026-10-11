package co.edu.unimagdalena.samaria360.catalog.domain.model;

/**
 * Restaurante o bar (RF10): tipo de cocina, aforo por turno, menú vegetariano (opcional en el esquema) y rango
 * de precio por plato.
 */
public record DetalleGastronomia(String tipoCocina, int aforoMaxTurno, Boolean menuVegetariano,
                                 String rangoPrecioPlato) implements DetalleCategoria {

    public DetalleGastronomia {
        Validaciones.texto(tipoCocina, "tipoCocina");
        Validaciones.positivo(aforoMaxTurno, "aforoMaxTurno");
        Validaciones.texto(rangoPrecioPlato, "rangoPrecioPlato");
    }

    @Override
    public Categoria categoria() {
        return Categoria.Gastronomia;
    }
}
