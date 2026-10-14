package co.edu.unimagdalena.samaria360.catalog.application.fake;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.ServicioRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Repositorio en memoria con el mismo filtro de visibilidad que el adaptador MongoDB. Registra los ids consultados. */
public class ServicioRepositoryEnMemoria implements ServicioRepositoryPort {

    private final List<ServicioComercial> servicios = new ArrayList<>();
    private final List<String> consultados = new ArrayList<>();

    public ServicioRepositoryEnMemoria con(ServicioComercial... nuevos) {
        servicios.addAll(List.of(nuevos));
        return this;
    }

    @Override
    public Optional<ServicioComercial> buscarVisible(String id) {
        consultados.add(id);
        return servicios.stream()
                .filter(servicio -> servicio.getId().filter(id::equals).isPresent())
                .filter(ServicioComercial::esVisibleEnBusquedaPublica)
                .findFirst();
    }

    public List<String> consultados() {
        return List.copyOf(consultados);
    }
}
