package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.GestionarAvisosUseCase;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Aviso;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaAviso;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaLista;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.RespuestaPagina;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto.SolicitudAviso;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.error.DatosInvalidosException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * CU «Crear y publicar aviso general» (RF40; WF-14) y su CU secundario «Consultar avisos generales». {@code GET /avisos}
 * es público (el banner); {@code /admin/avisos/**} es solo del Administrador (ver {@code SecurityConfig}). Contrato en
 * {@code docs/sad/api/ms-catalog.md}.
 */
@RestController
@RequestMapping("/api/v1/catalogo")
public class AvisoController {

    private static final String RUTA_ADMIN = "/api/v1/catalogo/admin/avisos/";

    private final GestionarAvisosUseCase gestionar;

    public AvisoController(GestionarAvisosUseCase gestionar) {
        this.gestionar = gestionar;
    }

    @GetMapping("/avisos")
    public RespuestaLista<RespuestaAviso> activos() {
        return new RespuestaLista<>(gestionar.activos().stream().map(RespuestaAviso::de).toList());
    }

    /** {@code pagina} llega como texto para responder igual a «abc» que a «-1»: 400 con {@code campos.pagina}. */
    @GetMapping("/admin/avisos")
    public RespuestaPagina<RespuestaAviso> pagina(@RequestParam(defaultValue = "0") String pagina) {
        return RespuestaPagina.de(gestionar.pagina(numero(pagina)), RespuestaAviso::de);
    }

    @PostMapping("/admin/avisos")
    public ResponseEntity<RespuestaAviso> publicar(@RequestBody SolicitudAviso solicitud) {
        Aviso aviso = gestionar.publicar(solicitud.mensaje(), solicitud.tipo());
        return ResponseEntity.created(URI.create(RUTA_ADMIN + aviso.id())).body(RespuestaAviso.de(aviso));
    }

    @PostMapping("/admin/avisos/{id}/despublicacion")
    public RespuestaAviso despublicar(@PathVariable String id) {
        return RespuestaAviso.de(gestionar.despublicar(id));
    }

    private static int numero(String pagina) {
        try {
            return Integer.parseInt(pagina.strip());
        } catch (NumberFormatException e) {
            throw new DatosInvalidosException(Map.of("pagina", "FORMATO"));
        }
    }
}
