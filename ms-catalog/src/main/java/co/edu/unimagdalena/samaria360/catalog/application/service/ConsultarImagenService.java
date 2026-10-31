package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarImagenUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.AlmacenImagenesPort;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ImagenNoEncontradaException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.IdentificadorObjeto;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ImagenAlmacenada;

import java.util.Objects;

/** Imagen de GridFS por su id (DEC-209). */
public class ConsultarImagenService implements ConsultarImagenUseCase {

    private final AlmacenImagenesPort imagenes;

    public ConsultarImagenService(AlmacenImagenesPort imagenes) {
        this.imagenes = Objects.requireNonNull(imagenes);
    }

    @Override
    public ImagenAlmacenada buscar(String id) {
        if (!IdentificadorObjeto.esValido(id)) {
            throw new ImagenNoEncontradaException(id);
        }
        return imagenes.buscar(id).orElseThrow(() -> new ImagenNoEncontradaException(id));
    }
}
