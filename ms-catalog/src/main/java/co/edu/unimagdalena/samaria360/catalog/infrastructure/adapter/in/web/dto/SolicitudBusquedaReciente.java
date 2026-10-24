package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.Categoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CondicionAccesibilidad;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CriteriosGuardados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.OrdenBusqueda;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ParametrosFiltrosCategoria;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cuerpo de POST /api/v1/catalogo/historial (RF60, DEC-88): los mismos nombres que los parámetros de
 * {@code GET /oferta}, más {@code usarUbicacion}. Sin {@code lat}, {@code lng}, {@code pagina} ni {@code tamano}: si
 * llegan, se ignoran como cualquier campo desconocido (por RF38 la ubicación no se guarda). Un enum desconocido o un
 * tipo inválido hacen que el cuerpo no se pueda leer (400 DATOS_INVALIDOS); las reglas de los criterios, las de la
 * búsqueda (400 CRITERIOS_INVALIDOS).
 */
public record SolicitudBusquedaReciente(String texto, Categoria categoria, BigDecimal precioMax, Boolean usarUbicacion,
                                        Double distanciaKm, List<CondicionAccesibilidad> accesibilidad,
                                        OrdenBusqueda orden, List<String> serviciosIncluidos, Integer huespedes,
                                        String tipoHabitacion, Boolean menuVegetariano, String tipoCocina,
                                        String nivelDificultad, BigDecimal duracionMaxHoras,
                                        String tipoVehiculoEquipo) {

    public CriteriosGuardados aCriterios() {
        return new CriteriosGuardados(texto, categoria, precioMax, Boolean.TRUE.equals(usarUbicacion), distanciaKm,
                accesibilidad, orden, new ParametrosFiltrosCategoria(serviciosIncluidos, huespedes, tipoHabitacion,
                menuVegetariano, tipoCocina, nivelDificultad, duracionMaxHoras, tipoVehiculoEquipo));
    }
}
