package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarOpcionesFiltroUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.OpcionesFiltroRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Categoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.OpcionesFiltro;

import java.util.Objects;

public class ConsultarOpcionesFiltroService implements ConsultarOpcionesFiltroUseCase {

    private final OpcionesFiltroRepositoryPort opciones;

    public ConsultarOpcionesFiltroService(OpcionesFiltroRepositoryPort opciones) {
        this.opciones = Objects.requireNonNull(opciones);
    }

    @Override
    public OpcionesFiltro consultar(Categoria categoria) {
        if (categoria == null) {
            throw new CriteriosBusquedaInvalidosException("Las opciones de filtro exigen categoria");
        }
        return opciones.opciones(categoria);
    }
}
