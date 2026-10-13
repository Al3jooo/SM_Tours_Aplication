package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.List;

/**
 * Hotel u hostal (RF10): tipo de habitación, número de habitaciones, capacidad y servicios incluidos. Los servicios
 * incluidos son un vocabulario cerrado ({@link ServicioIncluido}) y se guardan con su nombre canónico.
 */
public record DetalleAlojamiento(String tipoHabitacion, int numHabitaciones, int capacidadMaxHuespedes,
                                 List<String> serviciosIncluidos) implements DetalleCategoria {

    public DetalleAlojamiento {
        Validaciones.texto(tipoHabitacion, "tipoHabitacion");
        Validaciones.positivo(numHabitaciones, "numHabitaciones");
        Validaciones.positivo(capacidadMaxHuespedes, "capacidadMaxHuespedes");
        serviciosIncluidos = Validaciones.lista(serviciosIncluidos).stream()
                .map(servicio -> ServicioIncluido.desdeTexto(servicio).name())
                .distinct()
                .toList();
    }

    @Override
    public Categoria categoria() {
        return Categoria.Alojamiento;
    }
}
