package co.edu.unimagdalena.samaria360.booking.domain.service;

import co.edu.unimagdalena.samaria360.booking.domain.model.RolCuenta;

import java.util.OptionalLong;

/**
 * Vigencia de una sesión frente a la versión de la cuenta (DEC-29, ADR-008): un token con {@code ver} menor que la
 * versión conocida es de antes de un cambio de estado o de rol. Sin versión conocida (la cuenta no está en la
 * proyección) se acepta un Turista o un Prestador: modo permisivo hasta {@code exp}. Un Gestor o un Administrador sin
 * versión conocida se rechaza (DEC-110, cierre de L3): desde V15 de ms-auth toda cuenta de esos roles tiene evento. Un
 * token sin {@code ver} cuenta como 0.
 */
public class PoliticaSesion {

    /** @param rol el claim {@code rol} del token */
    public boolean vigente(OptionalLong versionConocida, long ver, String rol) {
        if (versionConocida.isEmpty()) {
            return !RolCuenta.privilegiado(rol);
        }
        return ver >= versionConocida.getAsLong();
    }
}
