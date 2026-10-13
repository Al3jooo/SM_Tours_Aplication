package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Categoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.OpcionesFiltro;

/** Opciones de «Más filtros» de una categoría (WF-05, RF10), parte del caso de uso Buscar oferta turística. */
public interface ConsultarOpcionesFiltroUseCase {

    /** @throws CriteriosBusquedaInvalidosException si no llega la categoría */
    OpcionesFiltro consultar(Categoria categoria);
}
