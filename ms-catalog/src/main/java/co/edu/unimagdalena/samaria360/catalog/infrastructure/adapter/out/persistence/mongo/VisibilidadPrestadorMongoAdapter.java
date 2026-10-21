package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.VisibilidadPrestadorPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/** {@code servicios.prestadorActivo} por {@code prestadorId} (índice de M004). */
@Component
public class VisibilidadPrestadorMongoAdapter implements VisibilidadPrestadorPort {

    private final MongoTemplate mongo;

    public VisibilidadPrestadorMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void fijarPrestadorActivo(String idPrestador, boolean activo) {
        mongo.updateMulti(Query.query(Criteria.where("prestadorId").is(idPrestador)),
                new Update().set("prestadorActivo", activo), "servicios");
    }
}
