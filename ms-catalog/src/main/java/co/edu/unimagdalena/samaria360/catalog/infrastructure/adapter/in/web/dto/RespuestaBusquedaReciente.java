package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.BusquedaReciente;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CriteriosGuardados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ParametrosFiltrosCategoria;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Una búsqueda de «Últimas búsquedas» (RF60): sus criterios con los nombres de los parámetros de {@code GET /oferta},
 * para repetirla con un clic (si {@code usarUbicacion} es true, el frontend vuelve a pedir la ubicación).
 */
public record RespuestaBusquedaReciente(String id, Criterios criterios, Instant fecha) {

    public static RespuestaBusquedaReciente de(BusquedaReciente busqueda) {
        return new RespuestaBusquedaReciente(busqueda.id(), Criterios.de(busqueda.criterios()), busqueda.fecha());
    }

    public record Criterios(String texto, String categoria, BigDecimal precioMax, boolean usarUbicacion,
                            Double distanciaKm, List<String> accesibilidad, String orden,
                            List<String> serviciosIncluidos, Integer huespedes, String tipoHabitacion,
                            Boolean menuVegetariano, String tipoCocina, String nivelDificultad,
                            BigDecimal duracionMaxHoras, String tipoVehiculoEquipo) {

        static Criterios de(CriteriosGuardados c) {
            ParametrosFiltrosCategoria f = c.masFiltros();
            return new Criterios(c.texto(), c.categoria() == null ? null : c.categoria().name(), c.precioMax(),
                    c.usarUbicacion(), c.distanciaKm(), c.accesibilidad().stream().map(Enum::name).toList(),
                    c.orden() == null ? null : c.orden().name(), f.serviciosIncluidos(), f.huespedes(),
                    f.tipoHabitacion(), f.menuVegetariano(), f.tipoCocina(), f.nivelDificultad(),
                    f.duracionMaxHoras(), f.tipoVehiculoEquipo());
        }
    }
}
