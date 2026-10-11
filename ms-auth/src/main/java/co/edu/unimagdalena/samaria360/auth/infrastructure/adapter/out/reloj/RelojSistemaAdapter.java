package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.reloj;

import co.edu.unimagdalena.samaria360.auth.application.port.out.RelojPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class RelojSistemaAdapter implements RelojPort {

    private final Clock reloj = Clock.systemUTC();

    @Override
    public Instant ahora() {
        return Instant.now(reloj);
    }
}
