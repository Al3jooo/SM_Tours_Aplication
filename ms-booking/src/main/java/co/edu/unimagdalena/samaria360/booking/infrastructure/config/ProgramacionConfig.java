package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tareas programadas: solo el generador de inventario demo (D5), y solo si está habilitado. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "samaria360.inventario-demo.habilitado", havingValue = "true")
public class ProgramacionConfig {
}
