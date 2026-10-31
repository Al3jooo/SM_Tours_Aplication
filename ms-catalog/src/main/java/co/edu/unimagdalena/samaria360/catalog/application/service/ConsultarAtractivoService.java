package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarAtractivoUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.AtractivoRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.AtractivoNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.IdentificadorObjeto;

import java.util.Objects;

/** Detalle público de un atractivo (DEC-217): visible mientras exista (DEC-87). Solo lee. */
public class ConsultarAtractivoService implements ConsultarAtractivoUseCase {

    private final AtractivoRepositoryPort atractivos;

    public ConsultarAtractivoService(AtractivoRepositoryPort atractivos) {
        this.atractivos = Objects.requireNonNull(atractivos);
    }

    @Override
    public AtractivoPublico detalle(String id) {
        if (!IdentificadorObjeto.esValido(id)) {
            throw new AtractivoNoEncontradoException(id);
        }
        return atractivos.buscar(id).orElseThrow(() -> new AtractivoNoEncontradoException(id));
    }
}
