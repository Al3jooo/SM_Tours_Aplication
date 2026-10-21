package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.security;

import co.edu.unimagdalena.samaria360.auth.application.port.in.VerificarSesionUseCase;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Después de la firma, {@code iss} y {@code exp}: el claim {@code ver} contra {@code usuario.version_cuenta} (DEC-29).
 * Un token sin {@code ver} cuenta como 0. También rechaza lo que no es un token de usuario de la API pública: uno de
 * servicio ({@code rol = SERVICIO}) o uno cuyo {@code sub} no es un UUID.
 */
public class ValidadorVersionSesion implements OAuth2TokenValidator<Jwt> {

    /** Código del error cuando el token es de antes de un cambio de estado o de rol (401 SESION_INVALIDADA). */
    public static final String SESION_INVALIDADA = "sesion_invalidada";

    private final VerificarSesionUseCase sesiones;

    public ValidadorVersionSesion(VerificarSesionUseCase sesiones) {
        this.sesiones = sesiones;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (JwtServicioAdapter.ROL_SERVICIO.equals(jwt.getClaimAsString(JwtTokenProviderAdapter.CLAIM_ROL))) {
            return fallo(OAuth2ErrorCodes.INVALID_TOKEN, "Un token de servicio no sirve para la API pública");
        }
        UUID idUsuario;
        try {
            idUsuario = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            return fallo(OAuth2ErrorCodes.INVALID_TOKEN, "El sujeto del token no es un usuario");
        }
        Object ver = jwt.getClaim(JwtTokenProviderAdapter.CLAIM_VERSION);
        long version = ver instanceof Number numero ? numero.longValue() : 0;
        if (!sesiones.sesionVigente(idUsuario, version)) {
            return fallo(SESION_INVALIDADA, "La cuenta cambió después de iniciar sesión");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static OAuth2TokenValidatorResult fallo(String codigo, String descripcion) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(codigo, descripcion, null));
    }
}
