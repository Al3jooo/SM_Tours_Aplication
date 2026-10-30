package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.ActualizacionPrecio.PrecioUnidad;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cuerpo de {@code PUT /api/v1/catalogo/prestador/servicios/{id}/precio} (RF32, DEC-184; contrato en
 * {@code docs/sad/api/ms-catalog.md}): {@code precioBase} en Tour, Gastronomía y Alquiler, o el {@code precioNoche} de
 * todas las unidades en Alojamiento, y la {@code version} leída (DEC-186). Cualquier otro campo, incluida la oferta, se
 * ignora (DEC-141, DEC-183).
 */
public record SolicitudPrecio(BigDecimal precioBase, List<Unidad> unidades, Integer version) {

    public record Unidad(String idUnidad, BigDecimal precioNoche) {
    }

    public List<PrecioUnidad> preciosUnidad() {
        return unidades == null ? null : unidades.stream()
                .map(unidad -> unidad == null ? null : new PrecioUnidad(unidad.idUnidad(), unidad.precioNoche()))
                .toList();
    }
}
