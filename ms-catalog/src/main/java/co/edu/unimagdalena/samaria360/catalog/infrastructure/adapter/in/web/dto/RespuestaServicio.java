package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.CondicionAccesibilidad;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleAlojamiento;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleAlquilerEquipo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleCategoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleGastronomia;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleTour;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DiaSemana;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * Detalle público de un servicio (WF-06, WF-10 y la consulta de ms-booking, ADR-007). Solo datos públicos: nada del
 * estado interno de publicación.
 *
 * @param descripcion               texto público del servicio (WF-06); puede ser null
 * @param imagenes                  rutas de las imágenes; la primera es la portada (RF48)
 * @param precioBase                precio «desde» (RF37); en un alojamiento, el menor precio por noche de sus unidades
 * @param detalleCategoria          la forma depende de la categoría; en Alojamiento trae las unidades (D1)
 * @param contacto                  teléfono y correo de atención al público del servicio (RF09, RF43); null si no hay
 * @param condicionesAccesibilidad  las declaradas (RF27), en el orden de {@link #ORDEN_ACCESIBILIDAD}; vacía si no hay
 * @param horarioAtencion           días y horarios como texto libre (RF58); null si no hay
 * @param direccion                 dirección del servicio (RF09, HC2); null en un servicio anterior sin dirección
 * @param rnt                       RNT del prestador, copiado al registrar el servicio (DEC-126, HC2); null si no hay
 * @param oferta                    días de operación y, en un tour, salidas (DEC-136); null en Gastronomía y Alquiler.
 *                                  ms-booking la lee de esta respuesta para materializar el inventario (DEC-135)
 */
public record RespuestaServicio(String id, String idPrestador, String nombreComercial, String categoria,
                                String descripcion, List<String> imagenes, BigDecimal precioBase, String moneda,
                                Object detalleCategoria, Contacto contacto, List<String> condicionesAccesibilidad,
                                String horarioAtencion, String direccion, String rnt, Oferta oferta) {

    /** Los precios del prototipo están en pesos colombianos. */
    public static final String MONEDA = "COP";

    /** Orden de presentación explícito, el de RF27; no depende de la declaración del enum. */
    static final List<CondicionAccesibilidad> ORDEN_ACCESIBILIDAD = List.of(CondicionAccesibilidad.MovilidadReducida,
            CondicionAccesibilidad.Visual, CondicionAccesibilidad.Auditiva);

    /** Ambos opcionales, como en el esquema. */
    public record Contacto(String telefono, String correo) {
    }

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

    /** @param salidas {@code HH:mm} en hora de Santa Marta; solo en Tour */
    public record Oferta(List<String> diasOperacion, @JsonInclude(JsonInclude.Include.NON_NULL) List<String> salidas) {

        static Oferta de(co.edu.unimagdalena.samaria360.catalog.domain.model.Oferta oferta) {
            return new Oferta(oferta.diasOperacion().stream().map(DiaSemana::name).toList(),
                    oferta.salidas().isEmpty() ? null : oferta.salidas().stream().map(LocalTime::toString).toList());
        }
    }

    public static RespuestaServicio de(ServicioComercial servicio) {
        return new RespuestaServicio(servicio.getId().orElseThrow(), servicio.getPrestadorId().toString(),
                servicio.getNombreComercial(), servicio.getCategoria().name(), servicio.getDescripcion().orElse(null),
                servicio.getImagenes(), servicio.getPrecioBase().orElse(null), MONEDA, detalle(servicio.getDetalle()),
                servicio.getContacto().map(contacto -> new Contacto(contacto.telefono(), contacto.correo()))
                        .orElse(null),
                ORDEN_ACCESIBILIDAD.stream().filter(servicio.getCondicionesAccesibilidad()::contains)
                        .map(CondicionAccesibilidad::name).toList(),
                servicio.getHorarioAtencion().orElse(null), servicio.getDireccion().orElse(null),
                servicio.getRnt().orElse(null), servicio.getOferta().map(Oferta::de).orElse(null));
    }

    static List<String> accesibilidad(ServicioComercial servicio) {
        return ORDEN_ACCESIBILIDAD.stream().filter(servicio.getCondicionesAccesibilidad()::contains)
                .map(CondicionAccesibilidad::name).toList();
    }

    static Object detalle(DetalleCategoria detalle) {
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
