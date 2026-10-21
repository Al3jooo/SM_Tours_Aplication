package co.edu.unimagdalena.samaria360.auth.application.port.out;

import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoAdministrativo;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;

import java.util.UUID;

/** Evento reclamado por el relevo para un destino; {@code intentos} son los que ya fallaron. */
public record EntregaPendiente(long idEvento, UUID idUsuario, Rol rol, EstadoAdministrativo estadoCuenta, long version,
                               int intentos) {
}
