package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.HistorialBusquedasUseCase;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaBusquedaReciente;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaLista;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.SolicitudBusquedaReciente;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * CU «Consultar historial de búsquedas» (RF60; DEC-56, DEC-88). Solo el rol Turista (ver {@code SecurityConfig}); es una
 * llamada aparte de {@code GET /oferta}, que sigue pública y sin token (DEC-47). Contrato en
 * {@code docs/sad/api/ms-catalog.md}.
 */
@RestController
@RequestMapping("/api/v1/catalogo/historial")
public class HistorialBusquedaController {

    private final HistorialBusquedasUseCase historial;

    public HistorialBusquedaController(HistorialBusquedasUseCase historial) {
        this.historial = historial;
    }

    /** 204 si la guarda, si solo actualiza su fecha y si no la guarda por no tener criterios. */
    @PostMapping
    public ResponseEntity<Void> registrar(@AuthenticationPrincipal Jwt jwt,
                                          @RequestBody SolicitudBusquedaReciente solicitud) {
        historial.registrar(UUID.fromString(jwt.getSubject()), solicitud.aCriterios());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public RespuestaLista<RespuestaBusquedaReciente> listar(@AuthenticationPrincipal Jwt jwt) {
        return new RespuestaLista<>(historial.listar(UUID.fromString(jwt.getSubject())).stream()
                .map(RespuestaBusquedaReciente::de).toList());
    }
}
