package co.edu.unimagdalena.samaria360.booking.application.port.out;

import java.time.Instant;

/** Fuente de la hora actual: permite probar RF44 en el límite de 24 horas y las fechas pasadas. */
public interface RelojPort {

    Instant ahora();
}
