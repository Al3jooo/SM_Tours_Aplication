package co.edu.unimagdalena.samaria360.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuración del JWT (samaria360.jwt.*). Las rutas de las llaves PEM son relativas al directorio
 * desde el que se ejecuta el servicio.
 */
@ConfigurationProperties("samaria360.jwt")
public record JwtPropiedades(String emisor, String llavePrivada, String llavePublica, Duration expiracion) {
}
