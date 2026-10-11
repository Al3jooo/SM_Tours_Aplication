package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ConsultarUsuarioActualUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.UsuarioActual;
import co.edu.unimagdalena.samaria360.auth.application.port.out.UsuarioRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.exception.UsuarioNoEncontradoException;

import java.util.Objects;
import java.util.UUID;

public class ConsultarUsuarioActualService implements ConsultarUsuarioActualUseCase {

    private final UsuarioRepositoryPort usuarios;

    public ConsultarUsuarioActualService(UsuarioRepositoryPort usuarios) {
        this.usuarios = Objects.requireNonNull(usuarios);
    }

    @Override
    public UsuarioActual consultar(UUID idUsuario) {
        return usuarios.buscarPorId(idUsuario)
                .map(u -> new UsuarioActual(u.getId(), u.getCorreo(), u.getRol(), u.getEstadoCuenta()))
                .orElseThrow(() -> new UsuarioNoEncontradoException(idUsuario));
    }
}
