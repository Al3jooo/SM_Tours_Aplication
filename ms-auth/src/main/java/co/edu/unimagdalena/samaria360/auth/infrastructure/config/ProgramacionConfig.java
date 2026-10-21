package co.edu.unimagdalena.samaria360.auth.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Tareas programadas: el relevo del outbox de cuentas (ADR-008). {@code spring.task.scheduling.pool.size} es 2, uno
 * por destino, para que un destino caído no retrase al otro.
 */
@Configuration
@EnableScheduling
public class ProgramacionConfig {
}
