package co.edu.unimagdalena.samaria360.auth.domain.exception;

import java.util.UUID;

/** No existe un usuario con el id indicado. */
public class UsuarioNoEncontradoException extends RuntimeException {

    private final UUID idUsuario;

    public UsuarioNoEncontradoException(UUID idUsuario) {
        super("Usuario no encontrado");
        this.idUsuario = idUsuario;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }
}
