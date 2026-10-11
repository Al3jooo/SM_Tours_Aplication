package co.edu.unimagdalena.samaria360.auth.infrastructure.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Llaves RS256 del JWT: ms-auth firma con la privada y valida con su propia pública, que también publica
 * en el JWKS para el API Gateway.
 */
@Configuration
public class JwtConfig {

    /** {@code kid} = huella RFC 7638 de la clave pública: estable mientras no se cambie la llave. */
    @Bean
    public RSAKey llaveJwt(JwtPropiedades propiedades) throws JOSEException {
        RSAPrivateKey privada = leer(propiedades.llavePrivada(), RsaKeyConverters.pkcs8());
        RSAPublicKey publica = leer(propiedades.llavePublica(), RsaKeyConverters.x509());
        if (privada instanceof RSAPrivateCrtKey crt && !crt.getModulus().equals(publica.getModulus())) {
            throw new IllegalStateException("La llave pública del JWT (" + propiedades.llavePublica()
                    + ") no corresponde a la llave privada (" + propiedades.llavePrivada() + ")");
        }
        return new RSAKey.Builder(publica)
                .privateKey(privada)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyIDFromThumbprint()
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey llaveJwt) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(llaveJwt)));
    }

    /** Valida firma RS256, exp/nbf e iss. */
    @Bean
    public JwtDecoder jwtDecoder(RSAKey llaveJwt, JwtPropiedades propiedades) throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(llaveJwt.toRSAPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(propiedades.emisor()));
        return decoder;
    }

    private static <T> T leer(String ruta, Converter<InputStream, T> conversor) {
        Path archivo = Path.of(ruta).toAbsolutePath().normalize();
        try (InputStream entrada = Files.newInputStream(archivo)) {
            return conversor.convert(entrada);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la llave del JWT en " + archivo
                    + " (revisa JWT_LLAVE_PRIVADA / JWT_LLAVE_PUBLICA e infra/keys/README.md)", e);
        }
    }
}
