package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.programador;

import co.edu.unimagdalena.samaria360.booking.application.port.in.CompletarReservasUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RF52: al arrancar y después cada 15 minutos ({@code samaria360.completar-reservas.cada}), pasa a Completada las
 * reservas Confirmadas que ya terminaron. Una reserva queda Completada a más tardar 15 minutos después de su
 * {@code fecha_fin}. Los errores van al log y la siguiente pasada vuelve a intentar.
 */
@Component
@ConditionalOnProperty(name = "samaria360.completar-reservas.habilitado", havingValue = "true", matchIfMissing = true)
public class CompletarReservasProgramador {

    private static final Logger LOG = LoggerFactory.getLogger(CompletarReservasProgramador.class);

    private final CompletarReservasUseCase completar;

    public CompletarReservasProgramador(CompletarReservasUseCase completar) {
        this.completar = completar;
    }

    @Scheduled(initialDelayString = "${samaria360.completar-reservas.espera-inicial:PT5S}",
            fixedDelayString = "${samaria360.completar-reservas.cada:PT15M}")
    public void completarTerminadas() {
        try {
            LOG.info("RF52: reservas completadas en esta pasada: {}", completar.completar());
        } catch (RuntimeException error) {
            LOG.warn("RF52: no se pudieron completar las reservas; se reintenta en la siguiente pasada: {}",
                    error.getMessage());
        }
    }
}
