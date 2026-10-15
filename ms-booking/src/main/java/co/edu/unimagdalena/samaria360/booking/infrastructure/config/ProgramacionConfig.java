package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Tareas programadas: RF52 (completar reservas, cada 15 min) y el generador de inventario demo (D5). Cada programador
 * se habilita con su propiedad.
 */
@Configuration
@EnableScheduling
public class ProgramacionConfig {
}
