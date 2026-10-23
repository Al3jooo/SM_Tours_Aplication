package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.AvisoRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Aviso;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.TipoAviso;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Colección {@code avisos}: {@code {_id: ObjectId, mensaje, tipo, activo, fechaPublicacion}} (M005). Los listados usan
 * los índices {@code avisos_activos} y {@code avisos_fecha}: fecha de publicación descendente y {@code _id} descendente.
 */
@Component
public class AvisoMongoAdapter implements AvisoRepositoryPort {

    private static final String AVISOS = "avisos";
    private static final Document RECIENTES_PRIMERO = new Document("fechaPublicacion", -1).append("_id", -1);

    private final MongoTemplate mongo;

    public AvisoMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public String nuevoId() {
        return new ObjectId().toHexString();
    }

    @Override
    public void insertar(Aviso aviso) {
        coleccion().insertOne(new Document("_id", new ObjectId(aviso.id()))
                .append("mensaje", aviso.mensaje())
                .append("tipo", aviso.tipo().name())
                .append("activo", aviso.activo())
                .append("fechaPublicacion", Date.from(aviso.fechaPublicacion())));
    }

    @Override
    public List<Aviso> activos() {
        List<Aviso> avisos = new ArrayList<>();
        coleccion().find(Filters.eq("activo", true)).sort(RECIENTES_PRIMERO)
                .forEach(documento -> avisos.add(aDominio(documento)));
        return avisos;
    }

    /** Una página fuera de rango llega vacía, con el total real. */
    @Override
    public PaginaResultados<Aviso> pagina(int pagina, int tamano) {
        long total = coleccion().countDocuments();
        long salto = (long) pagina * tamano;
        List<Aviso> avisos = new ArrayList<>();
        if (salto < total) {
            coleccion().find().sort(RECIENTES_PRIMERO).skip((int) salto).limit(tamano)
                    .forEach(documento -> avisos.add(aDominio(documento)));
        }
        return new PaginaResultados<>(avisos, pagina, tamano, total);
    }

    @Override
    public Optional<Aviso> buscar(String id) {
        return Optional.ofNullable(coleccion().find(Filters.eq("_id", new ObjectId(id))).first())
                .map(AvisoMongoAdapter::aDominio);
    }

    @Override
    public boolean despublicar(String id) {
        return coleccion().updateOne(Filters.and(Filters.eq("_id", new ObjectId(id)), Filters.eq("activo", true)),
                Updates.set("activo", false)).getMatchedCount() > 0;
    }

    private MongoCollection<Document> coleccion() {
        return mongo.getCollection(AVISOS);
    }

    private static Aviso aDominio(Document documento) {
        return new Aviso(documento.getObjectId("_id").toHexString(), documento.getString("mensaje"),
                TipoAviso.valueOf(documento.getString("tipo")), documento.getBoolean("activo"),
                documento.getDate("fechaPublicacion").toInstant());
    }
}
