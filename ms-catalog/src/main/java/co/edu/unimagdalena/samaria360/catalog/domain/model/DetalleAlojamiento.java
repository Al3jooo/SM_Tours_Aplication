package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.List;

/** Hotel u hostal (RF10): tipo de habitación, número de habitaciones, capacidad y servicios incluidos. */
public record DetalleAlojamiento(String tipoHabitacion, int numHabitaciones, int capacidadMaxHuespedes,
                                 List<String> serviciosIncluidos) implements DetalleCategoria {

    public DetalleAlojamiento {
        Validaciones.texto(tipoHabitacion, "tipoHabitacion");
        Validaciones.positivo(numHabitaciones, "numHabitaciones");
        Validaciones.positivo(capacidadMaxHuespedes, "capacidadMaxHuespedes");
        serviciosIncluidos = Validaciones.lista(serviciosIncluidos);
    }

    @Override
    public Categoria categoria() {
        return Categoria.Alojamiento;
    }
}
