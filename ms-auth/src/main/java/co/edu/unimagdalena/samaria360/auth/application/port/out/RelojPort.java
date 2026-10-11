package co.edu.unimagdalena.samaria360.auth.application.port.out;

import java.time.Instant;

/** Fuente de la hora actual; permite probar el bloqueo sin esperar 15 minutos. */
public interface RelojPort {

    Instant ahora();
}
