package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.BuscarOfertaUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.OfertaRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CriteriosBusqueda;
import co.edu.unimagdalena.samaria360.catalog.domain.model.OrdenBusqueda;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ResultadoOferta;

import java.util.Objects;

public class BuscarOfertaService implements BuscarOfertaUseCase {

    private final OfertaRepositoryPort ofertas;

    public BuscarOfertaService(OfertaRepositoryPort ofertas) {
        this.ofertas = Objects.requireNonNull(ofertas);
    }

    @Override
    public PaginaResultados<ResultadoOferta> buscar(CriteriosBusqueda criterios) {
        Objects.requireNonNull(criterios, "criterios");
        return ofertas.buscar(criterios.orden() != null ? criterios : criterios.conOrden(ordenPorDefecto(criterios)));
    }

    /** Por distancia si el turista compartió su ubicación (RF38); si no, por nombre. */
    static OrdenBusqueda ordenPorDefecto(CriteriosBusqueda criterios) {
        return criterios.ubicacion() != null ? OrdenBusqueda.Distancia : OrdenBusqueda.Nombre;
    }
}
