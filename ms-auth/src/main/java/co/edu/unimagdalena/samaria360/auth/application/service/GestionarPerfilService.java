package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoPerfil;
import co.edu.unimagdalena.samaria360.auth.application.port.in.GestionarPerfilUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PerfilActual;
import co.edu.unimagdalena.samaria360.auth.application.port.out.PerfilRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.exception.UsuarioNoEncontradoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.PerfilTurista;

import java.util.Objects;
import java.util.UUID;

/**
 * Perfil del turista (RF53). No sube {@code version_cuenta} ni escribe en el outbox: el token no lleva datos del
 * perfil y las proyecciones solo guardan estado y versión (DEC-79).
 */
public class GestionarPerfilService implements GestionarPerfilUseCase {

    private final PerfilRepositoryPort perfiles;

    public GestionarPerfilService(PerfilRepositoryPort perfiles) {
        this.perfiles = Objects.requireNonNull(perfiles);
    }

    @Override
    public PerfilActual consultar(UUID idTurista) {
        return perfiles.buscar(idTurista).orElseThrow(() -> new UsuarioNoEncontradoException(idTurista));
    }

    @Override
    public PerfilActual actualizar(UUID idTurista, ComandoPerfil comando) {
        PerfilTurista perfil = PerfilTurista.validar(comando.primerNombre(), comando.segundoNombre(),
                comando.primerApellido(), comando.segundoApellido(), comando.paisOrigen(),
                comando.categoriasInteres());
        if (!perfiles.actualizar(idTurista, perfil)) {
            throw new UsuarioNoEncontradoException(idTurista);
        }
        return consultar(idTurista);
    }
}
