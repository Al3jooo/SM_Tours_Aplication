package co.edu.unimagdalena.samaria360.catalog.domain.model;

/**
 * Alquiler de equipos o transporte (RF10): tipo de vehículo o equipo, unidades disponibles y condiciones de
 * alquiler (opcionales en el esquema).
 */
public record DetalleAlquilerEquipo(String tipoVehiculoEquipo, int unidadesDisponibles,
                                    String condicionesAlquiler) implements DetalleCategoria {

    public DetalleAlquilerEquipo {
        Validaciones.texto(tipoVehiculoEquipo, "tipoVehiculoEquipo");
        Validaciones.noNegativo(unidadesDisponibles, "unidadesDisponibles");
    }

    @Override
    public Categoria categoria() {
        return Categoria.AlquilerEquipo;
    }
}
