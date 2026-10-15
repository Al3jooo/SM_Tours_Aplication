package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarServicioUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.ServicioRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.OfertaNoDisponibleException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.IdentificadorObjeto;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;

import java.util.Objects;

public class ConsultarServicioService implements ConsultarServicioUseCase {

    private final ServicioRepositoryPort servicios;

    public ConsultarServicioService(ServicioRepositoryPort servicios) {
        this.servicios = Objects.requireNonNull(servicios);
    }

    /** Un id que no es un ObjectId tampoco existe: 404, igual que un servicio no visible. */
    @Override
    public ServicioComercial consultar(String id) {
        if (!IdentificadorObjeto.esValido(id)) {
            throw new OfertaNoDisponibleException(id);
        }
        return servicios.buscarVisible(id)
                .filter(ServicioComercial::esVisibleEnBusquedaPublica)
                .orElseThrow(() -> new OfertaNoDisponibleException(id));
    }
}
