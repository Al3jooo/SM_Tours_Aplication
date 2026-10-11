package co.edu.unimagdalena.samaria360.catalog.application.fake;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.OfertaRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CriteriosBusqueda;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ResultadoOferta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;
import co.edu.unimagdalena.samaria360.catalog.domain.service.PoliticaBusqueda;

import java.util.ArrayList;
import java.util.List;

/** Repositorio en memoria que aplica la misma política que el adaptador MongoDB. Registra los criterios recibidos. */
public class OfertaRepositoryEnMemoria implements OfertaRepositoryPort {

    private final List<ServicioComercial> servicios = new ArrayList<>();
    private final List<AtractivoPublico> atractivos = new ArrayList<>();
    private final List<CriteriosBusqueda> recibidos = new ArrayList<>();
    private final PoliticaBusqueda politica = new PoliticaBusqueda();

    public OfertaRepositoryEnMemoria con(ServicioComercial... nuevos) {
        servicios.addAll(List.of(nuevos));
        return this;
    }

    public OfertaRepositoryEnMemoria con(AtractivoPublico... nuevos) {
        atractivos.addAll(List.of(nuevos));
        return this;
    }

    @Override
    public PaginaResultados<ResultadoOferta> buscar(CriteriosBusqueda criterios) {
        recibidos.add(criterios);
        return politica.buscar(servicios, atractivos, criterios);
    }

    public CriteriosBusqueda ultimosCriterios() {
        return recibidos.get(recibidos.size() - 1);
    }
}
