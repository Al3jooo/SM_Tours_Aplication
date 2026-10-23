package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.PreguntaFrecuenteRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PreguntaFrecuente;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Colección {@code faq}: {@code {_id: ObjectId, titulo, cuerpo, fechaActualizacion}} (M005). Se escriben documentos
 * {@link Document} sin {@code _class}: el validador no admite campos adicionales.
 */
@Component
public class PreguntaFrecuenteMongoAdapter implements PreguntaFrecuenteRepositoryPort {

    private static final String FAQ = "faq";
    /** La misma del índice {@code faq_titulo} (M005): sin ella, MongoDB no usa el índice para ordenar. */
    private static final Collation ESPANOL = Collation.builder()
            .locale("es").collationStrength(CollationStrength.PRIMARY).build();

    private final MongoTemplate mongo;

    public PreguntaFrecuenteMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public String nuevoId() {
        return new ObjectId().toHexString();
    }

    @Override
    public List<PreguntaFrecuente> listar() {
        List<PreguntaFrecuente> preguntas = new ArrayList<>();
        coleccion().find().sort(new Document("titulo", 1).append("_id", 1)).collation(ESPANOL)
                .forEach(documento -> preguntas.add(aDominio(documento)));
        return preguntas;
    }

    @Override
    public long contar() {
        return coleccion().countDocuments();
    }

    @Override
    public void insertar(PreguntaFrecuente pregunta) {
        coleccion().insertOne(aDocumento(pregunta));
    }

    @Override
    public boolean reemplazar(PreguntaFrecuente pregunta) {
        return coleccion().replaceOne(Filters.eq("_id", new ObjectId(pregunta.id())), aDocumento(pregunta))
                .getMatchedCount() > 0;
    }

    @Override
    public boolean eliminar(String id) {
        return coleccion().deleteOne(Filters.eq("_id", new ObjectId(id))).getDeletedCount() > 0;
    }

    private MongoCollection<Document> coleccion() {
        return mongo.getCollection(FAQ);
    }

    private static Document aDocumento(PreguntaFrecuente pregunta) {
        return new Document("_id", new ObjectId(pregunta.id()))
                .append("titulo", pregunta.titulo())
                .append("cuerpo", pregunta.cuerpo())
                .append("fechaActualizacion", Date.from(pregunta.fechaActualizacion()));
    }

    private static PreguntaFrecuente aDominio(Document documento) {
        return new PreguntaFrecuente(documento.getObjectId("_id").toHexString(), documento.getString("titulo"),
                documento.getString("cuerpo"), documento.getDate("fechaActualizacion").toInstant());
    }
}
