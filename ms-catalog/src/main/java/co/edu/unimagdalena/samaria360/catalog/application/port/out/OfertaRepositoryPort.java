package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import co.edu.unimagdalena.samaria360.catalog.domain.model.CriteriosBusqueda;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ResultadoOferta;

/**
 * Consulta combinada de servicios y atractivos. Debe aplicar las reglas de {@code PoliticaBusqueda} (decisiones de
 * docs/trazabilidad.md) y ordenar con desempate estable. Implementación actual: una agregación de MongoDB
 * (ADR-004); evolución prevista: una colección de lectura, sin cambiar este puerto.
 */
public interface OfertaRepositoryPort {

    /** @param criterios con el orden ya resuelto (nunca nulo) */
    PaginaResultados<ResultadoOferta> buscar(CriteriosBusqueda criterios);
}
