package co.edu.unimagdalena.samaria360.booking.application.fake;

import co.edu.unimagdalena.samaria360.booking.application.port.out.RelojPort;

import java.time.Instant;

public class RelojFijo implements RelojPort {

    private Instant ahora;

    public RelojFijo(Instant ahora) {
        this.ahora = ahora;
    }

    public void fijar(Instant nuevo) {
        this.ahora = nuevo;
    }

    @Override
    public Instant ahora() {
        return ahora;
    }
}
