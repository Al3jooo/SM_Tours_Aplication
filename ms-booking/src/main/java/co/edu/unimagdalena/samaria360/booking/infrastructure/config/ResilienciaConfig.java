package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.out.catalogo.RespuestaInesperadaCatalogoException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit breaker {@code catalogo} (D4, ADR-007): se abre si fallan la mitad de las últimas 10 llamadas (con al menos
 * 5) y prueba de nuevo a los 10 s. Sin TimeLimiter ({@code disable-time-limiter}): los límites de tiempo son los
 * timeouts HTTP, y la llamada corre en el mismo hilo. Una respuesta inesperada del catálogo no lo abre.
 */
@Configuration
public class ResilienciaConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> circuitoCatalogo() {
        CircuitBreakerConfig circuito = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .ignoreExceptions(RespuestaInesperadaCatalogoException.class)
                .build();
        return fabrica -> fabrica.configure(constructor -> constructor.circuitBreakerConfig(circuito),
                "catalogo");
    }
}
