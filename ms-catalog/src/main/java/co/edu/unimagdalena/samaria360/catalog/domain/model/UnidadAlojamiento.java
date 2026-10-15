package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.math.BigDecimal;

/**
 * Unidad reservable de un alojamiento: un tipo de habitación con su cantidad, capacidad y precio por noche (decisión
 * D1 del CU Reservar servicio; WF-10). Una reserva es siempre de una sola unidad.
 *
 * @param idUnidad              ObjectId en texto; estable aunque la unidad se renombre (lo usa reservas_db)
 * @param numHabitaciones       habitaciones de este tipo
 * @param capacidadMaxHuespedes huéspedes por habitación
 * @param precioNoche           precio por noche y por habitación, en COP
 */
public record UnidadAlojamiento(String idUnidad, String nombre, String tipoHabitacion, int numHabitaciones,
                                int capacidadMaxHuespedes, BigDecimal precioNoche) {

    public static final int LONGITUD_MAXIMA_NOMBRE = 100;

    public UnidadAlojamiento {
        if (!IdentificadorObjeto.esValido(idUnidad)) {
            throw new IllegalArgumentException("idUnidad debe ser un ObjectId de 24 caracteres hexadecimales");
        }
        Validaciones.textoMaximo(nombre, LONGITUD_MAXIMA_NOMBRE, "nombre");
        Validaciones.texto(tipoHabitacion, "tipoHabitacion");
        Validaciones.positivo(numHabitaciones, "numHabitaciones");
        Validaciones.positivo(capacidadMaxHuespedes, "capacidadMaxHuespedes");
        Validaciones.positivo(precioNoche, "precioNoche");
    }
}
