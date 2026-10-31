package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarImagenUseCase;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ImagenAlmacenada;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Imágenes de GridFS (DEC-209, ADR-009), públicas como la ficha que las muestra. Una imagen no cambia nunca después de
 * guardarla (la galería no se edita en esta iteración): se puede guardar en caché un año.
 */
@RestController
@RequestMapping("/api/v1/catalogo/imagenes")
public class ImagenController {

    private static final CacheControl INMUTABLE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    private final ConsultarImagenUseCase consultar;

    public ImagenController(ConsultarImagenUseCase consultar) {
        this.consultar = consultar;
    }

    /** El tipo sale del formato detectado por la firma al registrarla, nunca del nombre del archivo. */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> imagen(@PathVariable String id) {
        ImagenAlmacenada imagen = consultar.buscar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.formato().tipoContenido()))
                .cacheControl(INMUTABLE)
                .body(imagen.contenido());
    }
}
