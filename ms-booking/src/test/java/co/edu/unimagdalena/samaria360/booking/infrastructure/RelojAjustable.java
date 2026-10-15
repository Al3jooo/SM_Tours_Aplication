package co.edu.unimagdalena.samaria360.booking.infrastructure;

import co.edu.unimagdalena.samaria360.booking.application.port.out.RelojPort;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Reloj de las pruebas de integración: fijo en {@link #INICIO} hasta que una prueba lo mueva (RF44, fechas pasadas).
 * Un hilo puede tener su propia hora ({@link #fijarEnEsteHilo}): así, en una carrera, la cancelación ve «a tiempo» y
 * RF52 ve «ya terminó».
 */
public class RelojAjustable implements RelojPort {

    /** Domingo 10 de marzo de 2030, 12:00 en Santa Marta. */
    public static final LocalDate HOY = LocalDate.of(2030, 3, 10);
    public static final Instant INICIO = HOY.atTime(12, 0).atZone(ZoneId.of("America/Bogota")).toInstant();

    private volatile Instant ahora = INICIO;
    private final ThreadLocal<Instant> delHilo = new ThreadLocal<>();

    public void fijar(Instant nuevo) {
        ahora = nuevo;
    }

    public void fijarEnEsteHilo(Instant nuevo) {
        delHilo.set(nuevo);
    }

    public void soltarEsteHilo() {
        delHilo.remove();
    }

    public void reiniciar() {
        ahora = INICIO;
        delHilo.remove();
    }

    @Override
    public Instant ahora() {
        Instant propio = delHilo.get();
        return propio != null ? propio : ahora;
    }
}
