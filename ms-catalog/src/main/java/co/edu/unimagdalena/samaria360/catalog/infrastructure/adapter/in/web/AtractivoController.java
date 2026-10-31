package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarAtractivoUseCase;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaAtractivo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CU «Consultar detalle de atractivo» (DEC-217): la ficha pública (WF-06 para atractivos), sin token. Un atractivo es
 * visible mientras exista (DEC-87).
 */
@RestController
@RequestMapping("/api/v1/catalogo/atractivos")
public class AtractivoController {

    private final ConsultarAtractivoUseCase consultar;

    public AtractivoController(ConsultarAtractivoUseCase consultar) {
        this.consultar = consultar;
    }

    @GetMapping("/{id}")
    public RespuestaAtractivo detalle(@PathVariable String id) {
        return RespuestaAtractivo.de(consultar.detalle(id));
    }
}
