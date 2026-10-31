package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.AlmacenImagenesPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.FormatoImagen;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ImagenAlmacenada;
import com.mongodb.MongoGridFSException;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Imágenes en GridFS, bucket {@code imagenes} de {@code catalog_db} ({@code imagenes.files} e {@code imagenes.chunks};
 * DEC-55, DEC-209, ADR-009). Usa el {@link GridFSBucket} del driver que ya trae spring-data-mongodb: sin dependencias
 * nuevas. El id de cada archivo lo fija el caso de uso, para poder escribirlo en el atractivo y borrarlo si el registro
 * falla. El formato detectado por la firma va en {@code metadata.formato}.
 */
@Component
public class ImagenGridFsAdapter implements AlmacenImagenesPort {

    static final String BUCKET = "imagenes";

    private final MongoTemplate mongo;

    public ImagenGridFsAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public String nuevoId() {
        return new ObjectId().toHexString();
    }

    @Override
    public void guardar(String id, FormatoImagen formato, String nombre, byte[] contenido) {
        GridFSUploadOptions opciones = new GridFSUploadOptions()
                .metadata(new Document("formato", formato.name()).append("tipoContenido", formato.tipoContenido()));
        bucket().uploadFromStream(new BsonObjectId(new ObjectId(id)), nombre == null || nombre.isBlank() ? id : nombre,
                new ByteArrayInputStream(contenido), opciones);
    }

    @Override
    public void borrar(String id) {
        try {
            bucket().delete(new ObjectId(id));
        } catch (MongoGridFSException e) {
            // No existe: el borrado es idempotente.
        }
    }

    /** Un archivo sin el formato en sus metadatos no lo escribió este adaptador: se trata como inexistente. */
    @Override
    public Optional<ImagenAlmacenada> buscar(String id) {
        ObjectId objectId = new ObjectId(id);
        GridFSFile archivo = bucket().find(Filters.eq("_id", objectId)).first();
        if (archivo == null || archivo.getMetadata() == null || archivo.getMetadata().getString("formato") == null) {
            return Optional.empty();
        }
        FormatoImagen formato = FormatoImagen.valueOf(archivo.getMetadata().getString("formato"));
        try (GridFSDownloadStream flujo = bucket().openDownloadStream(objectId)) {
            return Optional.of(new ImagenAlmacenada(formato, flujo.readAllBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private GridFSBucket bucket() {
        return GridFSBuckets.create(mongo.getDb(), BUCKET);
    }
}
