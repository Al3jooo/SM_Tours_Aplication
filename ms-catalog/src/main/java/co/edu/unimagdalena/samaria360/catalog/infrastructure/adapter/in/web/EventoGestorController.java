package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.GestionarEventosUseCase;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EventoTuristico;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaEventoGestion;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaPagina;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.SolicitudEvento;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * CU «Registrar evento en agenda» (RF25) y su flujo «Consultar eventos registrados». Solo el rol Gestor (ver
 * {@code SecurityConfig}, DEC-220). Sin edición ni borrado (DEC-215). Contrato en {@code docs/sad/api/ms-catalog.md}.
 */
@RestController
@RequestMapping("/api/v1/catalogo/gestor/eventos")
public class EventoGestorController {

    private static final String RUTA = "/api/v1/catalogo/gestor/eventos/";

    private final GestionarEventosUseCase gestionar;

    public EventoGestorController(GestionarEventosUseCase gestionar) {
        this.gestionar = gestionar;
    }

    /** 201 con el evento, ya visible en la agenda. {@code Location} como en {@code POST /admin/avisos}. */
    @PostMapping
    public ResponseEntity<RespuestaEventoGestion> registrar(@AuthenticationPrincipal Jwt jwt,
                                                            @RequestBody SolicitudEvento solicitud) {
        EventoTuristico evento = gestionar.registrar(UUID.fromString(jwt.getSubject()), solicitud.aBorrador());
        return ResponseEntity.created(URI.create(RUTA + evento.id())).body(RespuestaEventoGestion.de(evento));
    }

    /** {@code pagina} llega como texto para responder igual a «abc» que a «-1»: 400 con {@code campos.pagina}. */
    @GetMapping
    public RespuestaPagina<RespuestaEventoGestion> listar(@RequestParam(defaultValue = "0") String pagina) {
        return RespuestaPagina.de(gestionar.listar(ServicioPrestadorController.numero(pagina)),
                RespuestaEventoGestion::de);
    }
}
