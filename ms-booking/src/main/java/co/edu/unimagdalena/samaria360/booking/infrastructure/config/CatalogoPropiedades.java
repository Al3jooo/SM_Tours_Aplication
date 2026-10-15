package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * ms-catalog (samaria360.catalogo.*, D4).
 *
 * @param url             {@code MS_CATALOG_URL}; en Docker Compose, {@code http://ms-catalog:8082}
 * @param timeoutConexion 500 ms
 * @param timeoutLectura  1,5 s
 */
@ConfigurationProperties("samaria360.catalogo")
public record CatalogoPropiedades(String url, Duration timeoutConexion, Duration timeoutLectura) {
}
