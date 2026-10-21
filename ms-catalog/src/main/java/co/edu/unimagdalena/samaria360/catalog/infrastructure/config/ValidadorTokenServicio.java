package co.edu.unimagdalena.samaria360.catalog.infrastructure.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Para los endpoints internos (ADR-008): un token de servicio ({@code rol = SERVICIO}) solo vale si su {@code aud} es este
 * servicio; con otra {@code aud} es un 401. Un token de usuario pasa este validador y lo rechaza la autorización (403).
 */
public class ValidadorTokenServicio implements OAuth2TokenValidator<Jwt> {

    private final String audiencia;

    public ValidadorTokenServicio(String audiencia) {
        this.audiencia = audiencia;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        boolean esDeServicio = ValidadorVersionSesion.ROL_SERVICIO.equals(jwt.getClaimAsString("rol"));
        if (esDeServicio && (jwt.getAudience() == null || !jwt.getAudience().contains(audiencia))) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
                    "El token de servicio no es para " + audiencia, null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
