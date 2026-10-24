package co.edu.unimagdalena.samaria360.auth.domain.service;

import co.edu.unimagdalena.samaria360.auth.domain.exception.UltimoAdministradorException;
import co.edu.unimagdalena.samaria360.auth.domain.model.CambioRol;

import java.util.Collection;
import java.util.UUID;

/**
 * Siempre debe quedar al menos un Administrador activo (DEC-82). Recibe los Administradores activos ya bloqueados en la
 * transacción (DEC-106): si dos Administradores se degradan entre sí a la vez, el segundo ve el conjunto sin el primero
 * y recibe el 409.
 */
public class PoliticaAdministradores {

    public void verificarQueQuedeUno(Collection<UUID> administradoresActivos, CambioRol cambio) {
        if (!cambio.quitaAdministrador()) {
            return;
        }
        boolean quedaOtro = administradoresActivos.stream().anyMatch(id -> !id.equals(cambio.idUsuario()));
        if (!quedaOtro) {
            throw new UltimoAdministradorException();
        }
    }
}
