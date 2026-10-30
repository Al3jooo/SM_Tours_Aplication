package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CU «Actualizar tarifas» (RF32; DEC-183, DEC-184): valida los precios nuevos de un servicio con las reglas del registro
 * (DEC-138) y devuelve el servicio con esos precios.
 * <ul>
 *   <li>Tour, Gastronomía y Alquiler: {@code precioBase}.</li>
 *   <li>Alojamiento: el {@code precioNoche} de <b>todas</b> sus unidades, una vez cada una; el «desde» se deriva
 *   (DEC-127) y el {@code precioBase} recibido se ignora, como en el registro.</li>
 * </ul>
 * Solo cambia el precio: la oferta, las unidades y los demás datos no (DEC-183). Las reservas ya hechas conservan el
 * precio que copiaron al confirmarse (ms-booking, DEC-187).
 */
public final class ActualizacionPrecio {

    /** El precio nuevo de una unidad de alojamiento, sin validar. */
    public record PrecioUnidad(String idUnidad, BigDecimal precioNoche) {
    }

    private final CamposServicio campos = new CamposServicio();

    private ActualizacionPrecio() {
    }

    /**
     * @param version la versión del servicio que el Prestador leyó (DEC-186)
     * @throws ContenidoInvalidoException con un motivo por cada campo inválido
     */
    public static ServicioEditado aplicar(ServicioComercial servicio, BigDecimal precioBase, List<PrecioUnidad> unidades,
                                          Integer version) {
        return new ActualizacionPrecio().validar(Objects.requireNonNull(servicio), precioBase, unidades, version);
    }

    private ServicioEditado validar(ServicioComercial servicio, BigDecimal precioBase, List<PrecioUnidad> unidades,
                                    Integer version) {
        Integer esperada = campos.entero("version", version, 0, Integer.MAX_VALUE);
        BigDecimal precio = null;
        DetalleCategoria detalle = servicio.getDetalle();
        if (detalle instanceof DetalleAlojamiento alojamiento) {
            detalle = alojamiento(alojamiento, unidades);
        } else {
            precio = campos.decimal("precioBase", precioBase, RegistroServicio.PRECIO_MAXIMO);
        }
        campos.lanzarSiHayErrores();
        return new ServicioEditado(servicio.conPrecios(precio, detalle), esperada);
    }

    /** Las unidades conservan su orden, su id y sus datos; solo cambia el precio por noche. */
    private DetalleAlojamiento alojamiento(DetalleAlojamiento actual, List<PrecioUnidad> datos) {
        if (datos == null) {
            campos.agregar("unidades", MotivoInvalido.REQUERIDO);
            return null;
        }
        int antes = campos.cantidad();
        Map<String, BigDecimal> precios = new HashMap<>();
        for (int i = 0; i < datos.size(); i++) {
            String prefijo = "unidades[" + i + "]";
            PrecioUnidad dato = datos.get(i);
            if (dato == null) {
                campos.agregar(prefijo, MotivoInvalido.REQUERIDO);
                continue;
            }
            BigDecimal precio = campos.decimal(prefijo + ".precioNoche", dato.precioNoche(),
                    RegistroServicio.PRECIO_MAXIMO);
            String id = dato.idUnidad();
            if (id == null || id.isBlank()) {
                campos.agregar(prefijo + ".idUnidad", MotivoInvalido.REQUERIDO);
            } else if (actual.unidad(id).isEmpty()) {
                campos.agregar(prefijo + ".idUnidad", MotivoInvalido.NO_PERMITIDO);
            } else if (precios.containsKey(id)) {
                campos.agregar(prefijo + ".idUnidad", MotivoInvalido.DUPLICADO);
            } else {
                precios.put(id, precio);
            }
        }
        if (campos.cantidad() > antes) {
            return null;
        }
        // Todas las unidades, cada una una vez: una unidad sin precio nuevo es una lista incompleta.
        if (precios.size() < actual.unidades().size()) {
            campos.agregar("unidades", MotivoInvalido.CANTIDAD_MINIMA);
            return null;
        }
        List<UnidadAlojamiento> nuevas = new ArrayList<>();
        for (UnidadAlojamiento unidad : actual.unidades()) {
            nuevas.add(new UnidadAlojamiento(unidad.idUnidad(), unidad.nombre(), unidad.tipoHabitacion(),
                    unidad.numHabitaciones(), unidad.capacidadMaxHuespedes(), precios.get(unidad.idUnidad())));
        }
        return new DetalleAlojamiento(nuevas, actual.serviciosIncluidos());
    }
}
