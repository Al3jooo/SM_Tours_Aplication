package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.GestionarPreguntasFrecuentesUseCase;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PreguntaFrecuente;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaLista;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaPreguntaFrecuente;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.SolicitudPreguntaFrecuente;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * CU «Gestionar FAQs» (RF26, RF47; WF-13). {@code GET /faq} es público y va completo: el filtro de RF47 lo hace el
 * cliente (DEC-62). {@code /admin/faq/**} es solo del Administrador (ver {@code SecurityConfig}). Contrato en
 * {@code docs/sad/api/ms-catalog.md}. El id llega como texto: uno que no es un ObjectId es un 404, no un 400.
 */
@RestController
@RequestMapping("/api/v1/catalogo")
public class PreguntaFrecuenteController {

    private static final String RUTA_ADMIN = "/api/v1/catalogo/admin/faq/";

    private final GestionarPreguntasFrecuentesUseCase gestionar;

    public PreguntaFrecuenteController(GestionarPreguntasFrecuentesUseCase gestionar) {
        this.gestionar = gestionar;
    }

    @GetMapping("/faq")
    public RespuestaLista<RespuestaPreguntaFrecuente> listar() {
        return new RespuestaLista<>(gestionar.listar().stream().map(RespuestaPreguntaFrecuente::de).toList());
    }

    @PostMapping("/admin/faq")
    public ResponseEntity<RespuestaPreguntaFrecuente> crear(@RequestBody SolicitudPreguntaFrecuente solicitud) {
        PreguntaFrecuente pregunta = gestionar.crear(solicitud.titulo(), solicitud.cuerpo());
        return ResponseEntity.created(URI.create(RUTA_ADMIN + pregunta.id()))
                .body(RespuestaPreguntaFrecuente.de(pregunta));
    }

    @PutMapping("/admin/faq/{id}")
    public RespuestaPreguntaFrecuente editar(@PathVariable String id,
                                             @RequestBody SolicitudPreguntaFrecuente solicitud) {
        return RespuestaPreguntaFrecuente.de(gestionar.editar(id, solicitud.titulo(), solicitud.cuerpo()));
    }

    @DeleteMapping("/admin/faq/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        gestionar.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
