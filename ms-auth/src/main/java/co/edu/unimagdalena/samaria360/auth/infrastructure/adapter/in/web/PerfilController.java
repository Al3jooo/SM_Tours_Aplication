package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.auth.application.port.in.GestionarPerfilUseCase;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto.RespuestaPerfil;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto.SolicitudPerfil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * CU «Actualizar datos personales» y «Actualizar intereses turísticos» (RF53, DEC-79, DEC-84). Solo el rol Turista
 * (SecurityConfig); el turista es siempre el del claim {@code sub}.
 */
@RestController
@RequestMapping("/api/v1/auth/perfil")
public class PerfilController {

    private final GestionarPerfilUseCase perfil;

    /** Recibe el GestionarPerfilUseCase @Primary: el decorador transaccional, no el servicio puro. */
    public PerfilController(GestionarPerfilUseCase perfil) {
        this.perfil = perfil;
    }

    @GetMapping
    public RespuestaPerfil consultar(@AuthenticationPrincipal Jwt jwt) {
        return RespuestaPerfil.de(perfil.consultar(UUID.fromString(jwt.getSubject())));
    }

    /** Reemplaza el perfil completo; un {@code correo} en el cuerpo se ignora (DEC-84). */
    @PutMapping
    public RespuestaPerfil actualizar(@AuthenticationPrincipal Jwt jwt, @RequestBody SolicitudPerfil solicitud) {
        return RespuestaPerfil.de(perfil.actualizar(UUID.fromString(jwt.getSubject()), solicitud.aComando()));
    }
}
