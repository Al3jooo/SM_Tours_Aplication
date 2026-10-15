package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.port.in.ConsultarMisReservasUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ConsultarReservaUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.out.ReservaRepositoryPort;
import co.edu.unimagdalena.samaria360.booking.domain.exception.ReservaNoEncontradaException;
import co.edu.unimagdalena.samaria360.booking.domain.model.FiltroReservas;
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
    public PaginaReservas consultar(UUID idTurista, String desde, String hasta, String estados, int pagina) {
        ValidadorCampos validador = new ValidadorCampos();
        FiltroReservas filtro = FiltroReservas.validar(desde, hasta, estados, validador);
        if (pagina < 0) {
            validador.error("pagina", MotivoInvalido.VALOR_MINIMO);
        }
        validador.lanzarSiHayErrores();
        return reservas.listarDeTurista(idTurista, filtro, pagina, TAMANO_PAGINA);
    }

    @Override
    public Reserva consultar(UUID idReserva, UUID idTurista) {
        return reservas.buscarDeTurista(idReserva, idTurista).orElseThrow(ReservaNoEncontradaException::new);
    }
}
