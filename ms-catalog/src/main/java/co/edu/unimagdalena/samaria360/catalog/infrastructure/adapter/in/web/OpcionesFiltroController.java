package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarOpcionesFiltroUseCase;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Categoria;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaOpcionesFiltro;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Opciones de «Más filtros» (WF-05, RF10). Pública, como la búsqueda. */
@RestController
@RequestMapping("/api/v1/catalogo")
public class OpcionesFiltroController {

    private final ConsultarOpcionesFiltroUseCase consultarOpciones;

    public OpcionesFiltroController(ConsultarOpcionesFiltroUseCase consultarOpciones) {
        this.consultarOpciones = consultarOpciones;
    }

    /** @param categoria obligatoria; si falta, 400 CRITERIOS_INVALIDOS (lo valida el caso de uso) */
    @GetMapping("/filtros")
    public RespuestaOpcionesFiltro opciones(@RequestParam(required = false) Categoria categoria) {
        return RespuestaOpcionesFiltro.de(consultarOpciones.consultar(categoria));
    }
}
