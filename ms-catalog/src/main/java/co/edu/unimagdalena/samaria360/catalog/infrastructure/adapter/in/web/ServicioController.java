package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarServicioUseCase;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaServicio;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Detalle de un servicio (WF-06, WF-10). Público, como la búsqueda: no tiene privilegios para la red interna; ms-booking
 * lo consulta igual que cualquier cliente (ADR-007, Zero-Trust).
 */
@RestController
@RequestMapping("/api/v1/catalogo")
public class ServicioController {

    private final ConsultarServicioUseCase consultarServicio;

    public ServicioController(ConsultarServicioUseCase consultarServicio) {
        this.consultarServicio = consultarServicio;
    }

    /** 404 OFERTA_NO_DISPONIBLE si el id no es válido, no existe o el servicio no es visible (RF16, RF22). */
    @GetMapping("/servicios/{id}")
    public RespuestaServicio consultar(@PathVariable String id) {
        return RespuestaServicio.de(consultarServicio.consultar(id));
    }
}
