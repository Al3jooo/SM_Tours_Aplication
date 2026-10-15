package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.port.in.CompletarReservasUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.ReservaRepositoryPort;

import java.util.Objects;

/** RF52 con la hora de {@link RelojPort}: el mismo reloj que decide RF44, así las pruebas no esperan. */
public class CompletarReservasService implements CompletarReservasUseCase {

    private final ReservaRepositoryPort reservas;
    private final RelojPort reloj;

    public CompletarReservasService(ReservaRepositoryPort reservas, RelojPort reloj) {
        this.reservas = Objects.requireNonNull(reservas);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public int completar() {
        return reservas.completarTerminadas(reloj.ahora());
    }
}
