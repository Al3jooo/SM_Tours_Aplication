package co.edu.unimagdalena.samaria360.booking.infrastructure;

import co.edu.unimagdalena.samaria360.booking.application.port.out.RelojPort;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Reloj de las pruebas de integración: fijo en {@link #INICIO} hasta que una prueba lo mueva (RF44, fechas pasadas). */
public class RelojAjustable implements RelojPort {

    /** Domingo 10 de marzo de 2030, 12:00 en Santa Marta. */
    public static final LocalDate HOY = LocalDate.of(2030, 3, 10);
    public static final Instant INICIO = HOY.atTime(12, 0).atZone(ZoneId.of("America/Bogota")).toInstant();

    private volatile Instant ahora = INICIO;

    public void fijar(Instant nuevo) {
        ahora = nuevo;
    }

    public void reiniciar() {
        ahora = INICIO;
    }

    @Override
    public Instant ahora() {
        return ahora;
    }
}
