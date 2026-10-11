package co.edu.unimagdalena.samaria360.auth.infrastructure;

import co.edu.unimagdalena.samaria360.auth.application.port.out.TokenEmitido;
import co.edu.unimagdalena.samaria360.auth.application.port.out.TokenProviderPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;

/**
 * PostgreSQL real (misma imagen que docker-compose) y un emisor de tokens falso, porque el JWT llega en la
 * parte 4. El contenedor es un bean: las clases de prueba que importan esta configuración comparten el
 * contexto en caché y un único contenedor.
 */
@TestConfiguration(proxyBeanMethods = false)
public class InfraestructuraTestConfig {

    public static final Duration VIGENCIA_TOKEN = Duration.ofMinutes(60);

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }

    @Bean
    TokenProviderPort tokenProviderFalso() {
        return (usuario, ahora) -> new TokenEmitido("token-" + usuario.getId(), ahora.plus(VIGENCIA_TOKEN));
    }
}
