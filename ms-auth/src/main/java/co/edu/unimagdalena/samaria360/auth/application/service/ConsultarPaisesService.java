package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ConsultarPaisesUseCase;
import co.edu.unimagdalena.samaria360.auth.domain.model.CodigoPais;

import java.util.List;

/** La lista sale del dominio ({@link CodigoPais}), la única fuente de los países admitidos (decisión D5). */
public class ConsultarPaisesService implements ConsultarPaisesUseCase {

    private final List<String> codigos = CodigoPais.todos();

    @Override
    public List<String> consultar() {
        return codigos;
    }
}
