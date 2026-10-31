package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import co.edu.unimagdalena.samaria360.catalog.domain.model.FormatoImagen;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ImagenAlmacenada;

import java.util.Optional;

/** Imágenes de los atractivos en GridFS, en {@code catalog_db} (DEC-55, DEC-209, ADR-009). Ids: ObjectId en texto. */
public interface AlmacenImagenesPort {

    /** Un ObjectId nuevo, en su forma textual, para una imagen que todavía no se guardó. */
    String nuevoId();

    void guardar(String id, FormatoImagen formato, String nombre, byte[] contenido);

    /** Idempotente: borrar una imagen que no existe no es un error. */
    void borrar(String id);

    Optional<ImagenAlmacenada> buscar(String id);
}
