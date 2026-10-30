package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.UsuarioNoEncontradoException;

import java.util.UUID;

/**
 * CU «Actualizar datos comerciales» (RF43; DEC-188), parte de la cuenta: el perfil comercial del Prestador en
 * auth_db. La parte que ve el Turista (nombre y contacto de cada servicio) está en ms-catalog (DEC-189).
 */
public interface GestionarPerfilPrestadorUseCase {

    /** @throws UsuarioNoEncontradoException si no existe un prestador con ese id */
    PerfilPrestador consultar(UUID idPrestador);

    /**
     * Reemplaza el correo corporativo y el teléfono; lo demás no se edita (se ignora si llega).
     *
     * @throws DatosRegistroInvalidosException con un motivo por campo
     * @throws UsuarioNoEncontradoException    si no existe un prestador con ese id
     */
    PerfilPrestador actualizar(UUID idPrestador, String correoCorporativo, String telefono);
}
