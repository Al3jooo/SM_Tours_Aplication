package co.edu.unimagdalena.samaria360.auth.domain.exception;

import java.util.UUID;

/** No existe un turista o un prestador con ese id (404 CUENTA_NO_ENCONTRADA); un id de otro rol cuenta como inexistente. */
public class CuentaNoEncontradaException extends RuntimeException {

    private final UUID idUsuario;

    public CuentaNoEncontradaException(UUID idUsuario) {
        super("Cuenta no encontrada");
        this.idUsuario = idUsuario;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }
}
