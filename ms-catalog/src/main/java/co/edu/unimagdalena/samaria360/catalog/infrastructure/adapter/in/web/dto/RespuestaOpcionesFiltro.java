package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.OpcionesFiltro;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta de GET /api/v1/catalogo/filtros: opciones de «Más filtros» de una categoría (WF-05, RF10). Solo lleva
 * los campos de esa categoría.
 *
 * @param serviciosIncluidos nombres canónicos (WiFi, Desayuno, Piscina, Parqueadero); el frontend los traduce
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaOpcionesFiltro(String categoria, List<String> serviciosIncluidos, List<String> tiposHabitacion,
                                      RangoDto<Integer> huespedes, Boolean opcionVegetariana, List<String> tiposCocina,
                                      List<String> nivelesDificultad, RangoDto<BigDecimal> duracionHoras,
                                      List<String> tiposVehiculoEquipo) {

    public record RangoDto<T>(T minimo, T maximo) {
    }

    public static RespuestaOpcionesFiltro de(OpcionesFiltro opciones) {
        return new RespuestaOpcionesFiltro(opciones.categoria().name(),
                opciones.serviciosIncluidos() == null ? null
                        : opciones.serviciosIncluidos().stream().map(Enum::name).toList(),
                opciones.tiposHabitacion(),
                opciones.huespedes() == null ? null
                        : new RangoDto<>(opciones.huespedes().minimo(), opciones.huespedes().maximo()),
                opciones.opcionVegetariana(), opciones.tiposCocina(), opciones.nivelesDificultad(),
                opciones.duracionHoras() == null ? null
                        : new RangoDto<>(opciones.duracionHoras().minimo(), opciones.duracionHoras().maximo()),
                opciones.tiposVehiculoEquipo());
    }
}
