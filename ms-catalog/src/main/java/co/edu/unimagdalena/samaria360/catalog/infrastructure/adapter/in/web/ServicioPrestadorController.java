package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarMisServiciosUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.in.RegistrarServicioUseCase;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.ElementoServicioPrestador;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaPagina;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaServicioGestion;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.SolicitudServicio;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.error.DatosInvalidosException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * CU «Registrar servicio» (RF09, RF10, RF37) y su flujo «Consultar mis servicios» (DEC-129, DEC-143). Solo el rol
 * Prestador (ver {@code SecurityConfig}); el prestador es siempre el del claim {@code sub}. Contrato en
 * {@code docs/sad/api/ms-catalog.md}.
 */
@RestController
@RequestMapping("/api/v1/catalogo/prestador/servicios")
public class ServicioPrestadorController {

    private static final String RUTA = "/api/v1/catalogo/prestador/servicios/";

    private final RegistrarServicioUseCase registrar;
    private final ConsultarMisServiciosUseCase consultar;

    public ServicioPrestadorController(RegistrarServicioUseCase registrar, ConsultarMisServiciosUseCase consultar) {
        this.registrar = registrar;
        this.consultar = consultar;
    }

    /** 201 con el servicio Pendiente de aprobación (DEC-124). */
    @PostMapping
    public ResponseEntity<RespuestaServicioGestion> registrar(@AuthenticationPrincipal Jwt jwt,
                                                              @RequestBody SolicitudServicio solicitud) {
        ServicioComercial servicio = registrar.registrar(prestador(jwt), solicitud.aBorrador());
        return ResponseEntity.created(URI.create(RUTA + servicio.getId().orElseThrow()))
                .body(RespuestaServicioGestion.paraPrestador(servicio));
    }

    /** {@code pagina} llega como texto para responder igual a «abc» que a «-1»: 400 con {@code campos.pagina}. */
    @GetMapping
    public RespuestaPagina<ElementoServicioPrestador> listar(@AuthenticationPrincipal Jwt jwt,
                                                             @RequestParam(required = false) String estado,
                                                             @RequestParam(defaultValue = "0") String pagina) {
        return RespuestaPagina.de(consultar.listar(prestador(jwt), estado, numero(pagina)),
                ElementoServicioPrestador::de);
    }

    @GetMapping("/{id}")
    public RespuestaServicioGestion detalle(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return RespuestaServicioGestion.paraPrestador(consultar.detalle(prestador(jwt), id));
    }

    private static UUID prestador(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    static int numero(String pagina) {
        try {
            return Integer.parseInt(pagina.strip());
        } catch (NumberFormatException e) {
            throw new DatosInvalidosException(Map.of("pagina", "FORMATO"));
        }
    }
}
