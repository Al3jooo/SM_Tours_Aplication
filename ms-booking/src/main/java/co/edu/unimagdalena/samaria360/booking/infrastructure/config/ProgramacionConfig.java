package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Tareas programadas: RF52 (completar reservas, cada 15 min), que se habilita con su propiedad. El generador de
 * inventario demo (D5) ya no existe: la oferta se materializa bajo demanda (DEC-156).
 */
@Configuration
@EnableScheduling
public class ProgramacionConfig {
}
