package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.UsuarioNoEncontradoException;

import java.util.UUID;

/** CU «Actualizar datos personales» y «Actualizar intereses turísticos» (RF53, DEC-79): el perfil del turista. */
public interface GestionarPerfilUseCase {

    /** @throws UsuarioNoEncontradoException si no existe un turista con ese id */
    PerfilActual consultar(UUID idTurista);

    /**
     * Reemplaza el perfil completo (no es parcial); las categorías se reemplazan en la misma transacción (DEC-85).
     *
     * @throws DatosRegistroInvalidosException con un motivo por campo
     * @throws UsuarioNoEncontradoException    si no existe un turista con ese id
     */
    PerfilActual actualizar(UUID idTurista, ComandoPerfil comando);
}
