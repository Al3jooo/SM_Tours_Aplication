package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CriteriosBusqueda;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ResultadoOferta;

/** Caso de uso Buscar oferta turística (RF16, RF23, RF38, RF50, RF61): servicios y atractivos en una sola lista. */
public interface BuscarOfertaUseCase {

    /**
     * Sin orden explícito: por distancia si hay ubicación; si no, por nombre.
     *
     * @throws CriteriosBusquedaInvalidosException si los criterios no son válidos (al construirlos)
     */
    PaginaResultados<ResultadoOferta> buscar(CriteriosBusqueda criterios);
}
