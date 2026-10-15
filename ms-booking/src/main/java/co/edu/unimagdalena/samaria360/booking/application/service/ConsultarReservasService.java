package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.port.in.ConsultarMisReservasUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ConsultarReservaUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.out.ReservaRepositoryPort;
import co.edu.unimagdalena.samaria360.booking.domain.exception.ReservaNoEncontradaException;
import co.edu.unimagdalena.samaria360.booking.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.booking.domain.model.PaginaReservas;
import co.edu.unimagdalena.samaria360.booking.domain.model.Reserva;
import co.edu.unimagdalena.samaria360.booking.domain.model.ValidadorCampos;

import java.util.Objects;
import java.util.UUID;

/** Consultas de las reservas propias. Responden con lo guardado: nunca llaman al catálogo (RF32). */
public class ConsultarReservasService implements ConsultarMisReservasUseCase, ConsultarReservaUseCase {

    private final ReservaRepositoryPort reservas;

    public ConsultarReservasService(ReservaRepositoryPort reservas) {
        this.reservas = Objects.requireNonNull(reservas);
    }

    @Override
    public PaginaReservas consultar(UUID idTurista, int pagina) {
        if (pagina < 0) {
            ValidadorCampos validador = new ValidadorCampos();
            validador.error("pagina", MotivoInvalido.VALOR_MINIMO);
            validador.lanzarSiHayErrores();
        }
        return reservas.listarDeTurista(idTurista, pagina, TAMANO_PAGINA);
    }

    @Override
    public Reserva consultar(UUID idReserva, UUID idTurista) {
        return reservas.buscarDeTurista(idReserva, idTurista).orElseThrow(ReservaNoEncontradaException::new);
    }
}
