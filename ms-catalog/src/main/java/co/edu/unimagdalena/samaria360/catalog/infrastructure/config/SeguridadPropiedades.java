package co.edu.unimagdalena.samaria360.catalog.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Validación de los JWT emitidos por ms-auth (samaria360.seguridad.*).
 *
 * @param jwkSetUri JWKS de ms-auth (o del API Gateway más adelante)
 * @param emisor    claim {@code iss} esperado
 */
@ConfigurationProperties("samaria360.seguridad")
public record SeguridadPropiedades(String jwkSetUri, String emisor) {
}
