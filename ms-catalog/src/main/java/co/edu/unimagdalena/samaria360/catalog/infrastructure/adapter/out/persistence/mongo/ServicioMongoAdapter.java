package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.ServicioRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Detalle de un servicio visible: el mismo filtro de visibilidad que la búsqueda pública (RF16, RF22). */
@Component
public class ServicioMongoAdapter implements ServicioRepositoryPort {

    private static final String SERVICIOS = "servicios";

    private final MongoTemplate mongo;

    public ServicioMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<ServicioComercial> buscarVisible(String id) {
        Document filtro = new Document("_id", new ObjectId(id))
                .append("estadoPublicacion", "Publicado")
                .append("prestadorActivo", true);
        return Optional.ofNullable(mongo.getCollection(SERVICIOS).find(filtro).first())
                .map(ServicioDocumentoMapper::aDominio);
    }
}
