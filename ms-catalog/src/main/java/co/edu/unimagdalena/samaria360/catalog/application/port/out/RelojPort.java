package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import java.time.Instant;

/** Fuente de la hora actual; permite probar {@code fechaActualizacion} y {@code fechaPublicacion} con una hora fija. */
public interface RelojPort {

    Instant ahora();
}
