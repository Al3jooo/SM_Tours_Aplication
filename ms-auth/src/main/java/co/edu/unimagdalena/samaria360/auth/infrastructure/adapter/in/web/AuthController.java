package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoInicioSesion;
import co.edu.unimagdalena.samaria360.auth.application.port.in.ConsultarUsuarioActualUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.IniciarSesionUseCase;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto.RespuestaInicioSesion;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto.RespuestaUsuarioActual;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto.SolicitudInicioSesion;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Caso de uso Iniciar sesión (RF15). Solo depende de los puertos de entrada. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final IniciarSesionUseCase iniciarSesion;
    private final ConsultarUsuarioActualUseCase consultarUsuarioActual;

    /** Recibe el IniciarSesionUseCase @Primary: el decorador transaccional, no el servicio puro. */
    public AuthController(IniciarSesionUseCase iniciarSesion, ConsultarUsuarioActualUseCase consultarUsuarioActual) {
        this.iniciarSesion = iniciarSesion;
        this.consultarUsuarioActual = consultarUsuarioActual;
    }

    @PostMapping("/login")
    public RespuestaInicioSesion login(@Valid @RequestBody SolicitudInicioSesion solicitud) {
        return RespuestaInicioSesion.de(
                iniciarSesion.iniciarSesion(new ComandoInicioSesion(solicitud.correo(), solicitud.contrasena())));
    }

    @GetMapping("/me")
    public RespuestaUsuarioActual me(@AuthenticationPrincipal Jwt jwt) {
        return RespuestaUsuarioActual.de(consultarUsuarioActual.consultar(UUID.fromString(jwt.getSubject())));
    }
}
