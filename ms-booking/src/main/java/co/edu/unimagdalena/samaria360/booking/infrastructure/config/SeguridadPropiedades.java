package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Validación de los JWT emitidos por ms-auth (samaria360.seguridad.*), como en ms-catalog.
 *
 * @param jwkSetUri JWKS de ms-auth
 * @param emisor    claim {@code iss} esperado
 * @param audiencia {@code aud} que deben llevar los tokens de servicio de los endpoints internos (ADR-008)
 */
@ConfigurationProperties("samaria360.seguridad")
public record SeguridadPropiedades(String jwkSetUri, String emisor, String audiencia) {
}
