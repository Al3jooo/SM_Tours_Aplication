package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.auth.application.port.in.RegistrarPrestadorUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.RegistrarTuristaUseCase;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto.RespuestaRegistro;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto.SolicitudRegistroPrestador;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto.SolicitudRegistroTurista;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Caso de uso Registrarse (RF01, RF02; WF-02 y WF-03). Público. Responde siempre 202 con el mismo cuerpo, exista o no el
 * correo, y no inicia sesión (ADR-006). Recibe los decoradores transaccionales (@Primary).
 */
@RestController
@RequestMapping("/api/v1/auth/registro")
public class RegistroController {

    private final RegistrarTuristaUseCase registrarTurista;
    private final RegistrarPrestadorUseCase registrarPrestador;

    public RegistroController(RegistrarTuristaUseCase registrarTurista, RegistrarPrestadorUseCase registrarPrestador) {
        this.registrarTurista = registrarTurista;
        this.registrarPrestador = registrarPrestador;
    }

    @PostMapping("/turista")
    public ResponseEntity<RespuestaRegistro> turista(@RequestBody SolicitudRegistroTurista solicitud) {
        registrarTurista.registrar(solicitud.aComando());
        return ResponseEntity.accepted().body(RespuestaRegistro.RECIBIDO);
    }

    @PostMapping("/prestador")
    public ResponseEntity<RespuestaRegistro> prestador(@RequestBody SolicitudRegistroPrestador solicitud) {
        registrarPrestador.registrar(solicitud.aComando());
        return ResponseEntity.accepted().body(RespuestaRegistro.RECIBIDO);
    }
}
