package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleAlojamiento;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleAlquilerEquipo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleCategoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleGastronomia;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleTour;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detalle público de un servicio (WF-06, WF-10 y la consulta de ms-booking, ADR-007). Solo datos públicos: nada del
 * estado interno de publicación.
 *
 * @param descripcion      texto público del servicio (WF-06); puede ser null
 * @param imagenes         rutas de las imágenes; la primera es la portada (RF48)
 * @param precioBase       precio «desde» (RF37); en un alojamiento, el menor precio por noche de sus unidades
 * @param detalleCategoria la forma depende de la categoría; en Alojamiento trae las unidades (D1)
 */
public record RespuestaServicio(String id, String idPrestador, String nombreComercial, String categoria,
                                String descripcion, List<String> imagenes, BigDecimal precioBase, String moneda,
                                Object detalleCategoria) {

    /** Los precios del prototipo están en pesos colombianos. */
    public static final String MONEDA = "COP";

    public record Alojamiento(List<String> serviciosIncluidos, List<Unidad> unidades) {
    }

    /** @param precioNoche por noche y por habitación */
    public record Unidad(String idUnidad, String nombre, String tipoHabitacion, int numHabitaciones,
                         int capacidadMaxHuespedes, BigDecimal precioNoche) {
    }

    public record Gastronomia(String tipoCocina, int aforoMaxTurno, Boolean menuVegetariano, String rangoPrecioPlato) {
    }

    public record Tour(String puntoEncuentro, BigDecimal duracionHoras, String nivelDificultad, int aforoMaxGrupo) {
    }

    public record AlquilerEquipo(String tipoVehiculoEquipo, int unidadesDisponibles, String condicionesAlquiler) {
    }

    public static RespuestaServicio de(ServicioComercial servicio) {
        return new RespuestaServicio(servicio.getId().orElseThrow(), servicio.getPrestadorId().toString(),
                servicio.getNombreComercial(), servicio.getCategoria().name(), servicio.getDescripcion().orElse(null),
                servicio.getImagenes(), servicio.getPrecioBase().orElse(null), MONEDA, detalle(servicio.getDetalle()));
    }

    private static Object detalle(DetalleCategoria detalle) {
        if (detalle instanceof DetalleAlojamiento alojamiento) {
            return new Alojamiento(alojamiento.serviciosIncluidos(), alojamiento.unidades().stream()
                    .map(unidad -> new Unidad(unidad.idUnidad(), unidad.nombre(), unidad.tipoHabitacion(),
                            unidad.numHabitaciones(), unidad.capacidadMaxHuespedes(), unidad.precioNoche()))
                    .toList());
        }
        if (detalle instanceof DetalleGastronomia gastronomia) {
            return new Gastronomia(gastronomia.tipoCocina(), gastronomia.aforoMaxTurno(),
                    gastronomia.menuVegetariano(), gastronomia.rangoPrecioPlato());
        }
        if (detalle instanceof DetalleTour tour) {
            return new Tour(tour.puntoEncuentro(), tour.duracionHoras(), tour.nivelDificultad(), tour.aforoMaxGrupo());
        }
        DetalleAlquilerEquipo alquiler = (DetalleAlquilerEquipo) detalle;
        return new AlquilerEquipo(alquiler.tipoVehiculoEquipo(), alquiler.unidadesDisponibles(),
                alquiler.condicionesAlquiler());
    }
}
