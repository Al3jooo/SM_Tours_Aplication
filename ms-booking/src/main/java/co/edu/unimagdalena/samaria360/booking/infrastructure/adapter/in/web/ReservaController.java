package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.booking.application.port.in.CancelarReservaUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ComandoReserva;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ConsultarMisReservasUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ConsultarReservaUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ReservarServicioUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ResultadoReserva;
import co.edu.unimagdalena.samaria360.booking.domain.exception.ReservaNoEncontradaException;
import co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.dto.RespuestaPaginaReservas;
import co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.dto.RespuestaReserva;
import co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.dto.SolicitudReservaDto;
import co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.error.IdempotenciaRequeridaException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

/** Reservas del turista (RF18, RF20). Solo el rol Turista; el turista es el {@code sub} del JWT. */
@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

    private final ReservarServicioUseCase reservar;
    private final CancelarReservaUseCase cancelar;
    private final ConsultarMisReservasUseCase misReservas;
    private final ConsultarReservaUseCase consultar;

    public ReservaController(ReservarServicioUseCase reservar, CancelarReservaUseCase cancelar,
                             ConsultarMisReservasUseCase misReservas, ConsultarReservaUseCase consultar) {
        this.reservar = reservar;
        this.cancelar = cancelar;
        this.misReservas = misReservas;
        this.consultar = consultar;
    }

    /**
     * 201 con la reserva confirmada (RF20). Con una clave ya usada y el mismo cuerpo, 201 con la misma reserva y
     * {@code Idempotent-Replayed: true} (D6).
     */
    @PostMapping
    public ResponseEntity<RespuestaReserva> reservar(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestHeader(value = IDEMPOTENCY_KEY, required = false)
                                                     String clave,
                                                     @RequestBody SolicitudReservaDto cuerpo) {
        UUID claveIdempotencia = uuid(clave).orElseThrow(IdempotenciaRequeridaException::new);
        ResultadoReserva resultado = reservar.reservar(new ComandoReserva(turista(jwt), claveIdempotencia,
                cuerpo.idServicio(), cuerpo.idUnidad(), cuerpo.fechaEntrada(), cuerpo.fechaSalida(),
                cuerpo.horaInicio(), cuerpo.cantidad(), cuerpo.personas(), cuerpo.precioTotalEsperado()));
        ResponseEntity.BodyBuilder respuesta = ResponseEntity
                .created(URI.create("/api/v1/reservas/" + resultado.reserva().idReserva()));
        if (resultado.repetida()) {
            respuesta.header(IDEMPOTENT_REPLAYED, "true");
        }
        return respuesta.body(RespuestaReserva.de(resultado.reserva()));
    }

    /**
     * RF12 (del lado del turista): {@code desde} y {@code hasta} filtran por la fecha de la actividad; {@code estado}
     * admite uno o varios valores separados por coma.
     */
    @GetMapping("/mias")
    public RespuestaPaginaReservas mias(@AuthenticationPrincipal Jwt jwt,
                                        @RequestParam(required = false) String desde,
                                        @RequestParam(required = false) String hasta,
                                        @RequestParam(required = false) String estado,
                                        @RequestParam(defaultValue = "0") int pagina) {
        return RespuestaPaginaReservas.de(misReservas.consultar(turista(jwt), desde, hasta, estado, pagina));
    }

    /** 404 si no existe o si es de otro turista: no se revela cuál. */
    @GetMapping("/{id}")
    public RespuestaReserva consultar(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return RespuestaReserva.de(consultar.consultar(idReserva(id), turista(jwt)));
    }

    /** RF18 y RF44. Cancelar una reserva ya Cancelada responde 200 con ella, sin liberar cupos otra vez. */
    @PostMapping("/{id}/cancelacion")
    public RespuestaReserva cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return RespuestaReserva.de(cancelar.cancelar(idReserva(id), turista(jwt)));
    }

    private static UUID turista(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static UUID idReserva(String id) {
        return uuid(id).orElseThrow(ReservaNoEncontradaException::new);
    }

    private static Optional<UUID> uuid(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(valor.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
