package co.edu.unimagdalena.samaria360.auth.application.port.out;

import java.time.Instant;

public record TokenEmitido(String token, Instant expiraEn) {
}
