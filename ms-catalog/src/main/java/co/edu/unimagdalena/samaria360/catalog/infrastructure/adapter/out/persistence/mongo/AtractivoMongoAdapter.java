package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.AtractivoRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.GestionAtractivosPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
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
import java.util.Optional;

/**
 * Colección {@code atractivos} (validador v2, M012). Implementa la lectura pública (DEC-217) y la gestión del Gestor
 * (DEC-206). La edición y el cambio de estado son UPDATE condicionales sobre la versión leída que la suben (DEC-207).
 * El listado del panel ordena por {@code fechaRegistro} descendente; sin índice: la colección es pequeña.
 */
@Component
public class AtractivoMongoAdapter implements AtractivoRepositoryPort, GestionAtractivosPort {

    private static final String ATRACTIVOS = "atractivos";
    private static final Document RECIENTES_PRIMERO = new Document("fechaRegistro", -1).append("_id", -1);
    private static final Document SUBIR_VERSION = new Document("version", 1);

    private final MongoTemplate mongo;

    public AtractivoMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public String nuevoId() {
        return new ObjectId().toHexString();
    }

    @Override
    public void insertar(AtractivoPublico atractivo) {
        coleccion().insertOne(AtractivoDocumentoMapper.aDocumento(atractivo));
    }

    @Override
    public Optional<AtractivoPublico> buscar(String id) {
        return Optional.ofNullable(coleccion().find(Filters.eq("_id", new ObjectId(id))).first())
                .map(AtractivoDocumentoMapper::aDominio);
    }

    /** Una página fuera de rango llega vacía, con el total real. */
    @Override
    public PaginaResultados<AtractivoPublico> listar(int pagina, int tamano) {
        long total = coleccion().countDocuments();
        long salto = (long) pagina * tamano;
        List<AtractivoPublico> atractivos = new ArrayList<>();
        if (salto < total) {
            coleccion().find().sort(RECIENTES_PRIMERO).skip((int) salto).limit(tamano)
                    .forEach(documento -> atractivos.add(AtractivoDocumentoMapper.aDominio(documento)));
        }
        return new PaginaResultados<>(atractivos, pagina, tamano, total);
    }

    @Override
    public boolean actualizar(AtractivoPublico atractivo) {
        Document cambios = AtractivoDocumentoMapper.datos(atractivo);
        Document actualizacion = new Document("$set", conFecha(cambios, atractivo));
        Document vacios = AtractivoDocumentoMapper.vacios(atractivo);
        if (!vacios.isEmpty()) {
            actualizacion.append("$unset", vacios);
        }
        return escribir(atractivo, actualizacion);
    }

    @Override
    public boolean cambiarEstado(AtractivoPublico atractivo) {
        Document cambios = new Document("estadoOperativo", atractivo.getEstadoOperativo().orElseThrow().name());
        return escribir(atractivo, new Document("$set", conFecha(cambios, atractivo)));
    }

    /** {@code updateOne({_id, version}, cambios, $inc version)}. */
    private boolean escribir(AtractivoPublico atractivo, Document actualizacion) {
        Bson filtro = Filters.and(Filters.eq("_id", new ObjectId(atractivo.getId().orElseThrow())),
                Filters.eq("version", atractivo.getVersion()));
        return coleccion().updateOne(filtro, actualizacion.append("$inc", SUBIR_VERSION)).getMatchedCount() > 0;
    }

    private static Document conFecha(Document cambios, AtractivoPublico atractivo) {
        atractivo.getFechaActualizacion().ifPresent(fecha -> cambios.append("fechaActualizacion", Date.from(fecha)));
        return cambios;
    }

    private MongoCollection<Document> coleccion() {
        return mongo.getCollection(ATRACTIVOS);
    }
}
