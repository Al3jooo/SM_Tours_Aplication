package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.auth.application.port.in.CambiarRolUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoCambioRol;
import co.edu.unimagdalena.samaria360.auth.application.port.in.ConsultarPersonalUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PersonalResumen;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto.RespuestaPaginaCuentas;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto.SolicitudCambioRol;
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
 * Caso de uso Cambiar rol de cuenta (RF45; WF-12). Solo el rol Administrador: {@code /api/v1/auth/admin/**} en
 * {@code SecurityConfig}. Contrato en {@code docs/sad/api/ms-auth.md}. Solo depende de los puertos de entrada.
 */
@RestController
@RequestMapping("/api/v1/auth/admin")
public class CambioRolController {

    private final ConsultarPersonalUseCase consultar;
    private final CambiarRolUseCase cambiar;

    /** Recibe el CambiarRolUseCase @Primary: el decorador transaccional, no el servicio puro. */
    public CambioRolController(ConsultarPersonalUseCase consultar, CambiarRolUseCase cambiar) {
        this.consultar = consultar;
        this.cambiar = cambiar;
    }

    @GetMapping("/personal")
    public RespuestaPaginaCuentas<PersonalResumen> personal(@RequestParam(required = false) String rol,
                                                            @RequestParam(required = false) String q,
                                                            @RequestParam(defaultValue = "0") int pagina) {
        return RespuestaPaginaCuentas.de(consultar.personal(rol, q, pagina));
    }

    @PostMapping("/cuentas/{id}/cambio-rol")
    public PersonalResumen cambiarRol(@PathVariable UUID id, @RequestBody SolicitudCambioRol solicitud,
                                      @AuthenticationPrincipal Jwt jwt) {
        return cambiar.cambiarRol(new ComandoCambioRol(id, solicitud.rolNuevo(), solicitud.rolActual(),
                UUID.fromString(jwt.getSubject())));
    }
}
