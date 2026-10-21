package co.edu.unimagdalena.samaria360.booking.infrastructure;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT RS256 como los de ms-auth ({@code sub}, {@code rol}, {@code iss}), firmados con una llave generada para las
 * pruebas. Su JWKS lo publica {@link CatalogoSimulado}: ms-booking lo valida igual que en Docker Compose (ADR-005).
 * Nimbus ya llega con spring-boot-starter-oauth2-resource-server: no hay dependencias nuevas.
 */
public final class TokensDePrueba {

    private static final RSAKey LLAVE = generar();

    private TokensDePrueba() {
    }

    public static String jwks() {
        return new JWKSet(LLAVE.toPublicJWK()).toString();
    }

    /** Token de turista sin el claim {@code ver} (anterior a ADR-008): cuenta como 0. */
    public static String turista(UUID id) {
        return firmar(id, "Turista", null);
    }

    /** Token de turista con el claim {@code ver} (DEC-29). */
    public static String turista(UUID id, long ver) {
        return firmar(id, "Turista", ver);
    }

    /** JWT de servicio como el del relevo de ms-auth (ADR-008). */
    public static String servicio(String audiencia) {
        JWTClaimsSet datos = new JWTClaimsSet.Builder()
                .subject("ms-auth")
                .issuer("samaria360-auth")
                .claim("rol", "SERVICIO")
                .audience(java.util.List.of(audiencia))
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .build();
        return firmar(datos);
    }

    public static String prestador(UUID id) {
        return firmar(id, "Prestador", null);
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String firmar(UUID sujeto, String rol, Long ver) {
        JWTClaimsSet.Builder datos = new JWTClaimsSet.Builder()
                .subject(sujeto.toString())
                .issuer("samaria360-auth")
                .claim("rol", rol)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)));
        if (ver != null) {
            datos.claim("ver", ver);
        }
        return firmar(datos.build());
    }

    private static String firmar(JWTClaimsSet datos) {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(LLAVE.getKeyID()).build(), datos);
        try {
            jwt.sign(new RSASSASigner(LLAVE));
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        return jwt.serialize();
    }

    private static RSAKey generar() {
        try {
            return new RSAKeyGenerator(2048).keyID("prueba").generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
