package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.booking.application.port.in.ConsultarDisponibilidadUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ResumirDisponibilidadUseCase;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoDisponibilidad;
import co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.dto.RespuestaDisponibilidad;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Disponibilidad pública (RF11): se puede consultar sin iniciar sesión, como la búsqueda. */
@RestController
@RequestMapping("/api/v1/reservas/disponibilidad")
public class DisponibilidadController {

    private final ConsultarDisponibilidadUseCase disponibilidad;
    private final ResumirDisponibilidadUseCase resumen;

    public DisponibilidadController(ConsultarDisponibilidadUseCase disponibilidad,
                                    ResumirDisponibilidadUseCase resumen) {
        this.disponibilidad = disponibilidad;
        this.resumen = resumen;
    }

    /** Cupos de un servicio entre {@code desde} y {@code hasta} (por defecto, 30 días desde hoy; máximo 60). */
    @GetMapping
    public RespuestaDisponibilidad consultar(@RequestParam(required = false) String servicio,
                                             @RequestParam(required = false) String unidad,
                                             @RequestParam(required = false) String desde,
                                             @RequestParam(required = false) String hasta) {
        return RespuestaDisponibilidad.de(disponibilidad.consultar(servicio, unidad, desde, hasta));
    }

    /**
     * Para las tarjetas (§3 del plan): {@code servicios=id1,id2,…} (hasta 10). Por defecto, hoy y los 7 días
     * siguientes.
     */
    @GetMapping("/resumen")
    public Map<String, EstadoDisponibilidad> resumir(@RequestParam(required = false) List<String> servicios,
                                                     @RequestParam(required = false) String desde,
                                                     @RequestParam(required = false) String hasta) {
        return resumen.resumir(servicios, desde, hasta);
    }
}
