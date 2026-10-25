package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.GestionServiciosPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.ServicioRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoPublicacion;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Revision;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Colección {@code servicios} (esquema v3, M008). El detalle público aplica el mismo filtro de visibilidad que la
 * búsqueda (RF16, RF22). Los listados usan los índices de M008: {@code prestador_fecha} («Mis servicios», del más
 * reciente al más antiguo) y {@code admin_estado_fecha} (la cola del Administrador, del más antiguo al más reciente).
 */
@Component
public class ServicioMongoAdapter implements ServicioRepositoryPort, GestionServiciosPort {

    private static final String SERVICIOS = "servicios";
    private static final Document RECIENTES_PRIMERO = new Document("fechaRegistro", -1).append("_id", -1);
    private static final Document ANTIGUOS_PRIMERO = new Document("fechaRegistro", 1).append("_id", 1);

    private final MongoTemplate mongo;

    public ServicioMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<ServicioComercial> buscarVisible(String id) {
        Document filtro = new Document("_id", new ObjectId(id))
                .append("estadoPublicacion", "Publicado")
                .append("prestadorActivo", true);
        return Optional.ofNullable(coleccion().find(filtro).first())
                .map(ServicioDocumentoMapper::aDominio);
    }

    @Override
    public String nuevoId() {
        return new ObjectId().toHexString();
    }

    @Override
    public void insertar(ServicioComercial servicio) {
        coleccion().insertOne(ServicioDocumentoMapper.aDocumento(servicio));
    }

    @Override
    public Optional<ServicioComercial> buscar(String id) {
        return Optional.ofNullable(coleccion().find(Filters.eq("_id", new ObjectId(id))).first())
                .map(ServicioDocumentoMapper::aDominio);
    }

    @Override
    public PaginaResultados<ServicioComercial> listarDePrestador(UUID prestadorId, EstadoPublicacion estado,
                                                                 int pagina, int tamano) {
        Bson filtro = Filters.eq("prestadorId", prestadorId.toString());
        if (estado != null) {
            filtro = Filters.and(filtro, Filters.eq("estadoPublicacion", estado.name()));
        }
        return pagina(filtro, RECIENTES_PRIMERO, pagina, tamano);
    }

    @Override
    public PaginaResultados<ServicioComercial> listarPorEstado(EstadoPublicacion estado, int pagina, int tamano) {
        return pagina(Filters.eq("estadoPublicacion", estado.name()), ANTIGUOS_PRIMERO, pagina, tamano);
    }

    /** {@code updateOne({_id, estadoPublicacion: Pendiente}, $set)}: si ya se decidió, el filtro no coincide. */
    @Override
    public boolean decidir(String id, EstadoPublicacion estadoNuevo, Revision revision) {
        Document cambios = ServicioDocumentoMapper.revision(new Document("estadoPublicacion", estadoNuevo.name()),
                revision);
        return coleccion().updateOne(
                Filters.and(Filters.eq("_id", new ObjectId(id)), Filters.eq("estadoPublicacion", "Pendiente")),
                new Document("$set", cambios)).getMatchedCount() > 0;
    }

    @Override
    public void fijarPrestadorActivoEnServicio(String id, boolean activo) {
        coleccion().updateOne(Filters.eq("_id", new ObjectId(id)), Updates.set("prestadorActivo", activo));
    }

    /** Una página fuera de rango llega vacía, con el total real. */
    private PaginaResultados<ServicioComercial> pagina(Bson filtro, Document orden, int pagina, int tamano) {
        long total = coleccion().countDocuments(filtro);
        long salto = (long) pagina * tamano;
        List<ServicioComercial> servicios = new ArrayList<>();
        if (salto < total) {
            coleccion().find(filtro).sort(orden).skip((int) salto).limit(tamano)
                    .forEach(documento -> servicios.add(ServicioDocumentoMapper.aDominio(documento)));
        }
        return new PaginaResultados<>(servicios, pagina, tamano, total);
    }

    private MongoCollection<Document> coleccion() {
        return mongo.getCollection(SERVICIOS);
    }
}
