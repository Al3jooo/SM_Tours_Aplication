package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ConsultarPaisesUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * Países admitidos en el registro (RF01; decisión D5): códigos ISO 3166-1 alfa-2, sin nombres (el frontend los traduce).
 * Público, porque el registro es anónimo. La lista no cambia mientras corre el servicio: se puede guardar en caché.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class PaisesController {

    static final Duration VIGENCIA_CACHE = Duration.ofDays(1);

    private final ConsultarPaisesUseCase consultarPaises;

    public PaisesController(ConsultarPaisesUseCase consultarPaises) {
        this.consultarPaises = consultarPaises;
    }

    @GetMapping("/paises")
    public ResponseEntity<List<String>> paises() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(VIGENCIA_CACHE).cachePublic())
                .body(consultarPaises.consultar());
    }
}
