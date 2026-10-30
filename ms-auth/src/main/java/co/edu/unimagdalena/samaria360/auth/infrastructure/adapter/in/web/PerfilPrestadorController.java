package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.auth.application.port.in.GestionarPerfilPrestadorUseCase;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto.RespuestaPerfilPrestador;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto.SolicitudPerfilPrestador;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * CU «Actualizar datos comerciales» (RF43; DEC-188), parte de la cuenta. Solo el rol Prestador (SecurityConfig); el
 * prestador es siempre el del claim {@code sub}. Contrato en {@code docs/sad/api/ms-auth.md}.
 */
@RestController
@RequestMapping("/api/v1/auth/prestador/perfil")
public class PerfilPrestadorController {

    private final GestionarPerfilPrestadorUseCase perfil;

    public PerfilPrestadorController(GestionarPerfilPrestadorUseCase perfil) {
        this.perfil = perfil;
    }

    @GetMapping
    public RespuestaPerfilPrestador consultar(@AuthenticationPrincipal Jwt jwt) {
        return RespuestaPerfilPrestador.de(perfil.consultar(UUID.fromString(jwt.getSubject())));
    }

    @PutMapping
    public RespuestaPerfilPrestador actualizar(@AuthenticationPrincipal Jwt jwt,
                                               @RequestBody SolicitudPerfilPrestador solicitud) {
        return RespuestaPerfilPrestador.de(perfil.actualizar(UUID.fromString(jwt.getSubject()),
                solicitud.correoCorporativo(), solicitud.telefono()));
    }
}
