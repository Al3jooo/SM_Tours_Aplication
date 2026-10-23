package co.edu.unimagdalena.samaria360.catalog.application.fake;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.RelojPort;

import java.time.Duration;
import java.time.Instant;

/** Reloj que solo cambia cuando la prueba lo ajusta o lo adelanta. */
public class RelojFijo implements RelojPort {

    private Instant ahora;

    public RelojFijo(Instant ahora) {
        this.ahora = ahora;
    }

    public void ajustar(Instant ahora) {
        this.ahora = ahora;
    }

    public void avanzar(Duration tiempo) {
        this.ahora = ahora.plus(tiempo);
    }

    @Override
    public Instant ahora() {
        return ahora;
    }
}
