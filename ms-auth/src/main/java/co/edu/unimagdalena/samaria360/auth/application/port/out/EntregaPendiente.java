package co.edu.unimagdalena.samaria360.auth.application.port.out;

import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoAdministrativo;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;

import java.util.UUID;

/**
 * Evento reclamado por el relevo para un destino; {@code intentos} son los que ya fallaron. {@code rnt} solo en el de un
 * prestador (DEC-146).
 */
public record EntregaPendiente(long idEvento, UUID idUsuario, Rol rol, EstadoAdministrativo estadoCuenta, long version,
                               String rnt, int intentos) {
}
