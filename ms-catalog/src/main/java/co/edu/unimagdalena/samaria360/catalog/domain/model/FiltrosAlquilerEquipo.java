package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.math.BigDecimal;
import java.util.List;

/** @param tipoVehiculoEquipo tipo de vehículo o equipo exacto */
public record FiltrosAlquilerEquipo(String tipoVehiculoEquipo) implements FiltrosCategoria {

    public FiltrosAlquilerEquipo {
        tipoVehiculoEquipo = TextoFiltro.normalizar(tipoVehiculoEquipo, "tipoVehiculoEquipo");
    }

    @Override
    public Categoria categoria() {
        return Categoria.AlquilerEquipo;
    }

    @Override
    public List<String> camposActivos() {
        return tipoVehiculoEquipo == null ? List.of() : List.of("tipoVehiculoEquipo");
    }

    @Override
    public boolean cumple(DetalleCategoria detalle, BigDecimal precioMaximo) {
        return detalle instanceof DetalleAlquilerEquipo alquiler
                && TextoFiltro.coincide(tipoVehiculoEquipo, alquiler.tipoVehiculoEquipo());
    }
}
