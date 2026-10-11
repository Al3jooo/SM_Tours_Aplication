package co.edu.unimagdalena.samaria360.auth.application.fake;

import co.edu.unimagdalena.samaria360.auth.application.port.out.RelojPort;

import java.time.Instant;

public record RelojFijo(Instant ahora) implements RelojPort {
}
