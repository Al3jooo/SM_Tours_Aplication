package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.programador;

import co.edu.unimagdalena.samaria360.booking.application.port.in.AsegurarInventarioDemoUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Dispara el generador de inventario demo (D5) al arrancar y después una vez al día. Revisa cada minuto: si nunca
 * tuvo éxito (p. ej. ms-catalog todavía arrancaba) vuelve a intentar enseguida. Corre en segundo plano: no retrasa el
 * arranque, y sus errores van al log sin detener el servicio.
 * Solo existe con {@code samaria360.inventario-demo.habilitado=true} (Docker Compose, demostración).
 */
@Component
@ConditionalOnProperty(name = "samaria360.inventario-demo.habilitado", havingValue = "true")
public class InventarioDemoProgramador {

    private static final Logger LOG = LoggerFactory.getLogger(InventarioDemoProgramador.class);
    private static final Duration CADA = Duration.ofHours(24);

    private final AsegurarInventarioDemoUseCase generador;
    private volatile Instant ultimoExito;

    public InventarioDemoProgramador(AsegurarInventarioDemoUseCase generador) {
        this.generador = generador;
    }

    @Scheduled(initialDelayString = "${samaria360.inventario-demo.espera-inicial:PT10S}",
            fixedDelayString = "${samaria360.inventario-demo.revision:PT1M}")
    public void revisar() {
        Instant ahora = Instant.now();
        if (ultimoExito != null && ahora.isBefore(ultimoExito.plus(CADA))) {
            return;
        }
        try {
            int nuevas = generador.asegurar();
            ultimoExito = ahora;
            LOG.info("Inventario demo al día: {} filas nuevas", nuevas);
        } catch (RuntimeException error) {
            LOG.warn("No se pudo generar el inventario demo; se reintenta en un minuto: {}", error.getMessage());
        }
    }
}
