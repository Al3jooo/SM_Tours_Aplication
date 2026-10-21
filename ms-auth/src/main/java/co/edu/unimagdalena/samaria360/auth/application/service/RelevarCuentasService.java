package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.port.in.RelevarCuentasUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.out.DestinoCuentasPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.EntregaFallidaException;
import co.edu.unimagdalena.samaria360.auth.application.port.out.EntregaPendiente;
import co.edu.unimagdalena.samaria360.auth.application.port.out.OutboxPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.auth.domain.model.Destino;
import co.edu.unimagdalena.samaria360.auth.domain.service.PoliticaReintento;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Relevo del outbox (ADR-008). Una pasada por destino: reclama las entregas vencidas (solo la última de cada usuario) y
 * las entrega en orden. Ante el primer fallo programa el reintento con espera creciente, devuelve a la cola las que
 * quedaron sin intentar y termina la pasada: un destino caído no se insiste en la misma pasada, y como cada destino se
 * releva con su propio hilo, tampoco bloquea al otro. Supuesto de ADR-008: cada par (destino, usuario) se entrega de a
 * uno (un hilo por destino, la última versión por usuario, arriendo con {@code FOR UPDATE SKIP LOCKED}).
 */
public class RelevarCuentasService implements RelevarCuentasUseCase {

    /** Mayor que el tiempo máximo de una entrega (0,5 s de conexión y 1,5 s de lectura). */
    static final Duration ARRIENDO = Duration.ofSeconds(30);
    static final int LOTE = 50;

    private final OutboxPort outbox;
    private final DestinoCuentasPort destinos;
    private final RelojPort reloj;
    private final PoliticaReintento politica;

    public RelevarCuentasService(OutboxPort outbox, DestinoCuentasPort destinos, RelojPort reloj,
                                 PoliticaReintento politica) {
        this.outbox = Objects.requireNonNull(outbox);
        this.destinos = Objects.requireNonNull(destinos);
        this.reloj = Objects.requireNonNull(reloj);
        this.politica = Objects.requireNonNull(politica);
    }

    @Override
    public int relevar(Destino destino) {
        Instant ahora = reloj.ahora();
        List<EntregaPendiente> entregas = outbox.reclamar(destino, ahora, ARRIENDO, LOTE);
        int entregadas = 0;
        for (int i = 0; i < entregas.size(); i++) {
            EntregaPendiente entrega = entregas.get(i);
            try {
                destinos.enviar(destino, entrega);
            } catch (EntregaFallidaException fallo) {
                Instant proximo = reloj.ahora().plus(politica.espera(entrega.intentos() + 1));
                outbox.registrarFallo(destino, entrega, proximo, mensaje(fallo));
                outbox.liberar(destino, entregas.subList(i + 1, entregas.size()), reloj.ahora());
                return entregadas;
            }
            outbox.marcarEntregada(destino, entrega, reloj.ahora());
            entregadas++;
        }
        return entregadas;
    }

    private static String mensaje(EntregaFallidaException fallo) {
        String texto = fallo.getMessage() == null ? fallo.getClass().getSimpleName() : fallo.getMessage();
        return texto.length() > 500 ? texto.substring(0, 500) : texto;
    }
}
