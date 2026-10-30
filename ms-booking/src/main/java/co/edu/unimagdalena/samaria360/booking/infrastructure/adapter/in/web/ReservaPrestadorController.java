package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.booking.application.port.in.ConsultarReservasPrestadorUseCase;
import co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.dto.RespuestaPaginaReservasPrestador;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * CU «Consultar historial» del lado del Prestador (RF12; DEC-191, DEC-192). Solo el rol Prestador (ver
 * {@code SecurityConfig}); el prestador es el {@code sub} del JWT. Contrato en {@code docs/sad/api/ms-booking.md}.
 */
@RestController
public class ReservaPrestadorController {

    private final ConsultarReservasPrestadorUseCase consultar;

    public ReservaPrestadorController(ConsultarReservasPrestadorUseCase consultar) {
        this.consultar = consultar;
    }

    /**
     * Mismos filtros que {@code /mias} ({@code desde} y {@code hasta} por la fecha de la actividad; {@code estado} con
     * uno o varios valores separados por coma) más {@code servicio}.
     */
    @GetMapping("/api/v1/reservas/prestador")
    public RespuestaPaginaReservasPrestador listar(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam(required = false) String desde,
                                                   @RequestParam(required = false) String hasta,
                                                   @RequestParam(required = false) String estado,
                                                   @RequestParam(required = false) String servicio,
                                                   @RequestParam(defaultValue = "0") int pagina) {
        return RespuestaPaginaReservasPrestador.de(consultar.consultarDelPrestador(UUID.fromString(jwt.getSubject()),
                desde, hasta, estado, servicio, pagina));
    }
}
