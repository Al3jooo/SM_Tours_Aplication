package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarAgendaUseCase;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaEvento;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaPagina;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** CU «Consultar agenda de eventos» (RF25). Público, sin token (DEC-239; ver {@code SecurityConfig}). */
@RestController
public class AgendaController {

    private final ConsultarAgendaUseCase agenda;

    public AgendaController(ConsultarAgendaUseCase agenda) {
        this.agenda = agenda;
    }

    /** {@code pagina} llega como texto para responder igual a «abc» que a «-1»: 400 con {@code campos.pagina}. */
    @GetMapping("/api/v1/catalogo/eventos")
    public RespuestaPagina<RespuestaEvento> agenda(@RequestParam(defaultValue = "0") String pagina) {
        return RespuestaPagina.de(agenda.agenda(ServicioPrestadorController.numero(pagina)), RespuestaEvento::de);
    }
}
