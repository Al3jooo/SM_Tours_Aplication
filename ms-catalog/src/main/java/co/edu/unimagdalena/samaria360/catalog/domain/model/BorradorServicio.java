package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos de un servicio tal como los envía el prestador, sin validar (RF09, RF10, RF37; DEC-130 a DEC-133).
 * {@link RegistroServicio} los valida campo por campo. No trae lo que fija el servidor (DEC-141): el prestador, el
 * estado, el RNT, los {@code idUnidad} ni el {@code precioBase} de un alojamiento.
 *
 * @param detalleCategoria los campos de todas las categorías: solo se leen los de {@code categoria}
 * @param oferta           solo en Tour y Alojamiento (DEC-136)
 */
public record BorradorServicio(String nombreComercial, String categoria, String descripcion, String direccion,
                               DatosCoordenadas coordenadas, DatosContacto contacto,
                               List<String> condicionesAccesibilidad, String horarioAtencion, BigDecimal precioBase,
                               DatosDetalle detalleCategoria, DatosOferta oferta) {

    public record DatosCoordenadas(Double longitud, Double latitud) {
    }

    public record DatosContacto(String telefono, String correo) {
    }

    public record DatosUnidad(String nombre, String tipoHabitacion, Integer numHabitaciones,
                              Integer capacidadMaxHuespedes, BigDecimal precioNoche) {
    }

    public record DatosDetalle(List<DatosUnidad> unidades, List<String> serviciosIncluidos, String tipoCocina,
                               Integer aforoMaxTurno, Boolean menuVegetariano, String rangoPrecioPlato,
                               String puntoEncuentro, BigDecimal duracionHoras, String nivelDificultad,
                               Integer aforoMaxGrupo, String tipoVehiculoEquipo, Integer unidadesDisponibles,
                               String condicionesAlquiler) {
    }

    /** @param salidas horas {@code HH:mm} */
    public record DatosOferta(List<String> diasOperacion, List<String> salidas) {
    }
}
