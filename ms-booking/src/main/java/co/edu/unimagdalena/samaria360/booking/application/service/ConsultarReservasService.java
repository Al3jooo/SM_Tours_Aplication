package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.port.in.ConsultarMisReservasUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ConsultarReservasPrestadorUseCase;
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

/**
 * Consultas de las reservas propias (CU «Consultar historial», RF12): las del Turista y las de los servicios del
 * Prestador. Responden con lo guardado: nunca llaman al catálogo (RF32).
 */
public class ConsultarReservasService
        implements ConsultarMisReservasUseCase, ConsultarReservasPrestadorUseCase, ConsultarReservaUseCase {

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

    /** Las reservas guardan el dueño del servicio al confirmarse ({@code id_prestador}, DEC-191). */
    @Override
    public PaginaReservas consultarDelPrestador(UUID idPrestador, String desde, String hasta, String estados,
                                                String servicio, int pagina) {
        ValidadorCampos validador = new ValidadorCampos();
        FiltroReservas filtro = FiltroReservas.validarDelPrestador(desde, hasta, estados, servicio, validador);
        if (pagina < 0) {
            validador.error("pagina", MotivoInvalido.VALOR_MINIMO);
        }
        validador.lanzarSiHayErrores();
        return reservas.listarDePrestador(idPrestador, filtro, pagina, TAMANO_PAGINA);
    }

    @Override
    public Reserva consultar(UUID idReserva, UUID idTurista) {
        return reservas.buscarDeTurista(idReserva, idTurista).orElseThrow(ReservaNoEncontradaException::new);
    }
}
