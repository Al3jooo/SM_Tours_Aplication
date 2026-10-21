package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.auth.application.port.in.AdministrarCuentasUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoValidacion;
import co.edu.unimagdalena.samaria360.auth.application.port.in.ConsultarCuentasUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PrestadorDetalle;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PrestadorResumen;
import co.edu.unimagdalena.samaria360.auth.application.port.in.TuristaResumen;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto.RespuestaPaginaCuentas;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto.SolicitudValidacion;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Caso de uso Administrar cuentas (RF28, RF22, RF62; WF-12). Solo el rol Administrador (ver {@code SecurityConfig}).
 * Contrato en {@code docs/sad/api/ms-auth.md}. Solo depende de los puertos de entrada.
 */
@RestController
@RequestMapping("/api/v1/auth/admin")
public class AdminCuentasController {

    private final ConsultarCuentasUseCase consultar;
    private final AdministrarCuentasUseCase administrar;

    /** Recibe el AdministrarCuentasUseCase @Primary: el decorador transaccional, no el servicio puro. */
    public AdminCuentasController(ConsultarCuentasUseCase consultar, AdministrarCuentasUseCase administrar) {
        this.consultar = consultar;
        this.administrar = administrar;
    }

    @GetMapping("/prestadores")
    public RespuestaPaginaCuentas<PrestadorResumen> prestadores(@RequestParam(required = false) String estado,
                                                                @RequestParam(required = false) String q,
                                                                @RequestParam(defaultValue = "0") int pagina) {
        return RespuestaPaginaCuentas.de(consultar.prestadores(estado, q, pagina));
    }

    @GetMapping("/prestadores/{id}")
    public PrestadorDetalle prestador(@PathVariable UUID id) {
        return consultar.prestador(id);
    }

    @PostMapping("/prestadores/{id}/validacion")
    public PrestadorDetalle validar(@PathVariable UUID id, @RequestBody SolicitudValidacion solicitud,
                                    @AuthenticationPrincipal Jwt jwt) {
        return administrar.validar(new ComandoValidacion(id, solicitud.decision(), solicitud.justificacion(),
                UUID.fromString(jwt.getSubject())));
    }

    @PostMapping("/prestadores/{id}/inactivacion")
    public PrestadorDetalle inactivar(@PathVariable UUID id) {
        return administrar.inactivar(id);
    }

    @GetMapping("/turistas")
    public RespuestaPaginaCuentas<TuristaResumen> turistas(@RequestParam(required = false) String estado,
                                                           @RequestParam(required = false) String q,
                                                           @RequestParam(defaultValue = "0") int pagina) {
        return RespuestaPaginaCuentas.de(consultar.turistas(estado, q, pagina));
    }

    @PostMapping("/turistas/{id}/desactivacion")
    public TuristaResumen desactivar(@PathVariable UUID id) {
        return administrar.desactivar(id);
    }

    @PostMapping("/turistas/{id}/reactivacion")
    public TuristaResumen reactivar(@PathVariable UUID id) {
        return administrar.reactivar(id);
    }
}
