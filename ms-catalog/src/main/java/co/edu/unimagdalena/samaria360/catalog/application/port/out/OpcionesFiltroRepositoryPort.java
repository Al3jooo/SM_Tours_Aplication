package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import co.edu.unimagdalena.samaria360.catalog.domain.model.Categoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.OpcionesFiltro;

/**
 * Valores de {@code detalleCategoria} de los servicios visibles en la búsqueda pública (publicados y de prestador
 * activo, RF16 y RF22) de una categoría.
 */
public interface OpcionesFiltroRepositoryPort {

    OpcionesFiltro opciones(Categoria categoria);
}
