package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.AdministrarServiciosUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.in.AdministrarServiciosUseCase.ServicioEnRevision;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.ElementoServicioAdmin;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaPagina;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaServicioGestion;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.SolicitudAprobacion;
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
 * CU «Aprobar o rechazar servicio» (RF21; DEC-128, DEC-144, DEC-145) con «Consultar listado pendiente» y «Justificar
 * rechazo de servicio». Solo el Administrador ({@code /admin/**} en {@code SecurityConfig}). Contrato en
 * {@code docs/sad/api/ms-catalog.md}.
 */
@RestController
@RequestMapping("/api/v1/catalogo/admin/servicios")
public class ServicioAdminController {

    private final AdministrarServiciosUseCase administrar;

    public ServicioAdminController(AdministrarServiciosUseCase administrar) {
        this.administrar = administrar;
    }

    /** Sin {@code estado}, la cola de Pendientes, del más antiguo al más reciente. */
    @GetMapping
    public RespuestaPagina<ElementoServicioAdmin> listar(@RequestParam(required = false) String estado,
                                                         @RequestParam(defaultValue = "0") String pagina) {
        return RespuestaPagina.de(administrar.listar(estado, ServicioPrestadorController.numero(pagina)),
                ElementoServicioAdmin::de);
    }

    @GetMapping("/{id}")
    public RespuestaServicioGestion detalle(@PathVariable String id) {
        return respuesta(administrar.detalle(id));
    }

    @PostMapping("/{id}/aprobacion")
    public RespuestaServicioGestion decidir(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
                                            @RequestBody SolicitudAprobacion solicitud) {
        return respuesta(administrar.decidir(id, solicitud.decision(), solicitud.justificacion(),
                UUID.fromString(jwt.getSubject())));
    }

    private static RespuestaServicioGestion respuesta(ServicioEnRevision enRevision) {
        return RespuestaServicioGestion.paraAdministrador(enRevision.servicio(), enRevision.estadoCuentaPrestador());
    }
}
