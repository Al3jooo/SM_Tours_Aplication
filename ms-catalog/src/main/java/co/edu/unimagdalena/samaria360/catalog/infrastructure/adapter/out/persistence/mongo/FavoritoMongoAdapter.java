package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.FavoritoRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Favorito;
import co.edu.unimagdalena.samaria360.catalog.domain.model.TipoOferta;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Colección {@code favoritos}: {@code {_id, turistaId, entidadId, tipoEntidad, fechaCreacion}} (Entregable 2, §2.4, más
 * {@code fechaCreacion}; M007). La API usa {@code Servicio} y {@code Atractivo}; la colección, {@code servicio} y
 * {@code atractivo}. Se escriben documentos {@link Document} sin {@code _class}: el validador no admite campos
 * adicionales.
 */
@Component
public class FavoritoMongoAdapter implements FavoritoRepositoryPort {

    private static final String FAVORITOS = "favoritos";

    private final MongoTemplate mongo;

    public FavoritoMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<Favorito> buscar(UUID turistaId, TipoOferta tipo, String entidadId) {
        return Optional.ofNullable(coleccion().find(clave(turistaId, tipo, entidadId)).first())
                .map(FavoritoMongoAdapter::aDominio);
    }

    @Override
    public List<Favorito> listar(UUID turistaId, int maximo) {
        List<Favorito> favoritos = new ArrayList<>();
        coleccion().find(Filters.eq("turistaId", turistaId.toString()))
                .sort(new Document("fechaCreacion", -1).append("_id", -1)).limit(maximo)
                .forEach(documento -> favoritos.add(aDominio(documento)));
        return favoritos;
    }

    @Override
    public long contar(UUID turistaId) {
        return coleccion().countDocuments(Filters.eq("turistaId", turistaId.toString()));
    }

    @Override
    public boolean insertar(Favorito favorito) {
        try {
            coleccion().insertOne(new Document("_id", new ObjectId())
                    .append("turistaId", favorito.turistaId().toString())
                    .append("entidadId", favorito.entidadId())
                    .append("tipoEntidad", tipoEntidad(favorito.tipo()))
                    .append("fechaCreacion", Date.from(favorito.fechaCreacion())));
            return true;
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public void eliminar(UUID turistaId, TipoOferta tipo, String entidadId) {
        coleccion().deleteOne(clave(turistaId, tipo, entidadId));
    }

    private MongoCollection<Document> coleccion() {
        return mongo.getCollection(FAVORITOS);
    }

    /** Las mismas claves del índice único {@code favoritos_turista_entidad}. */
    private static Bson clave(UUID turistaId, TipoOferta tipo, String entidadId) {
        return Filters.and(Filters.eq("turistaId", turistaId.toString()),
                Filters.eq("tipoEntidad", tipoEntidad(tipo)), Filters.eq("entidadId", entidadId));
    }

    private static String tipoEntidad(TipoOferta tipo) {
        return tipo.name().toLowerCase(Locale.ROOT);
    }

    private static Favorito aDominio(Document documento) {
        TipoOferta tipo = "atractivo".equals(documento.getString("tipoEntidad")) ? TipoOferta.Atractivo
                : TipoOferta.Servicio;
        return new Favorito(UUID.fromString(documento.getString("turistaId")), tipo, documento.getString("entidadId"),
                documento.getDate("fechaCreacion").toInstant());
    }
}
