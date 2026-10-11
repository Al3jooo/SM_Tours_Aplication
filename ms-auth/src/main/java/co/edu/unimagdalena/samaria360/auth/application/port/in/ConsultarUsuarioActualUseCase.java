package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.exception.UsuarioNoEncontradoException;

import java.util.UUID;

/** Consulta los datos del usuario identificado por el claim {@code sub} del JWT. */
public interface ConsultarUsuarioActualUseCase {

    /** @throws UsuarioNoEncontradoException si no existe un usuario con ese id */
    UsuarioActual consultar(UUID idUsuario);
}
