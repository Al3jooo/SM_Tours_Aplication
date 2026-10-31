package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ImagenNoEncontradaException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ImagenAlmacenada;

/** Sirve una imagen guardada en GridFS (DEC-209, ADR-009): pública, como la ficha que la muestra. */
public interface ConsultarImagenUseCase {

    /** @throws ImagenNoEncontradaException si no existe o el id no es un ObjectId */
    ImagenAlmacenada buscar(String id);
}
