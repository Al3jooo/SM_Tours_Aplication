package co.edu.unimagdalena.samaria360.auth.domain.service;

import java.util.OptionalLong;

/**
 * Vigencia de una sesión frente a la versión de la cuenta (DEC-29, ADR-008): un token con {@code ver} menor que la
 * versión conocida es de antes de un cambio de estado o de rol. Sin versión conocida (la cuenta no está en la
 * proyección) se acepta: modo permisivo hasta {@code exp}. Un token sin {@code ver} cuenta como 0.
 */
public class PoliticaSesion {

    public boolean vigente(OptionalLong versionConocida, long ver) {
        return versionConocida.isEmpty() || ver >= versionConocida.getAsLong();
    }
}
