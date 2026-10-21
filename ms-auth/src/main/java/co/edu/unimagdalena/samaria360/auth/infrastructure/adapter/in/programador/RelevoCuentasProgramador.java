package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.programador;

import co.edu.unimagdalena.samaria360.auth.application.port.in.RelevarCuentasUseCase;
import co.edu.unimagdalena.samaria360.auth.domain.model.Destino;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Relevo del outbox (ADR-008): cada {@code samaria360.outbox.cada} entrega los eventos pendientes. Un método por destino,
 * cada uno en su propio hilo ({@code spring.task.scheduling.pool.size = 2}): un destino caído no bloquea al otro, y
 * dentro de un destino nunca hay dos pasadas a la vez (el supuesto de orden de ADR-008). Los errores van al log y la
 * siguiente pasada vuelve a intentar.
 */
@Component
@ConditionalOnProperty(name = "samaria360.outbox.habilitado", havingValue = "true", matchIfMissing = true)
public class RelevoCuentasProgramador {

    private static final Logger LOG = LoggerFactory.getLogger(RelevoCuentasProgramador.class);

    private final RelevarCuentasUseCase relevo;

    public RelevoCuentasProgramador(RelevarCuentasUseCase relevo) {
        this.relevo = relevo;
    }

    @Scheduled(initialDelayString = "${samaria360.outbox.espera-inicial:PT5S}",
            fixedDelayString = "${samaria360.outbox.cada:PT1S}")
    public void relevarCatalogo() {
        relevar(Destino.MS_CATALOG);
    }

    @Scheduled(initialDelayString = "${samaria360.outbox.espera-inicial:PT5S}",
            fixedDelayString = "${samaria360.outbox.cada:PT1S}")
    public void relevarReservas() {
        relevar(Destino.MS_BOOKING);
    }

    private void relevar(Destino destino) {
        try {
            int entregadas = relevo.relevar(destino);
            if (entregadas > 0) {
                LOG.info("Relevo de cuentas a {}: {} entregadas", destino.texto(), entregadas);
            }
        } catch (RuntimeException error) {
            LOG.warn("Relevo de cuentas a {}: la pasada falló y se reintenta: {}", destino.texto(), error.getMessage());
        }
    }
}
