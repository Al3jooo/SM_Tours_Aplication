package co.edu.unimagdalena.samaria360.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit breaker por ruta (un circuito por servicio): si un servicio falla, sus llamadas se cortan sin afectar
 * a los demás (ADR-005). Los timeouts del cliente HTTP están en application.yml (2 s de conexión, 5 s de respuesta).
 */
@Configuration
public class ResilienciaConfig {

    /** Mayor que el response-timeout (5 s): manda el timeout del cliente HTTP. El de Resilience4j por defecto es 1 s. */
    static final Duration TIEMPO_LIMITE = Duration.ofSeconds(6);

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> configuracionCircuitos() {
        return fabrica -> fabrica.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(TIEMPO_LIMITE).build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .build())
                .build());
    }
}
