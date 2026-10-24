package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.OfertaReferenciadaPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoPublicacion;
import co.edu.unimagdalena.samaria360.catalog.domain.model.TipoOferta;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lee de {@code servicios} y {@code atractivos} lo que muestra «Mis favoritos» (DEC-86). {@code entidadId} se guarda
 * como texto (Entregable 2) y aquí se convierte a ObjectId para el {@code $in}. La visibilidad de un servicio es la de
 * la búsqueda pública (Publicado y {@code prestadorActivo}); un atractivo es visible siempre que exista.
 */
@Component
public class OfertaReferenciadaMongoAdapter implements OfertaReferenciadaPort {

    private final MongoTemplate mongo;

    public OfertaReferenciadaMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Map<String, Referencia> buscar(TipoOferta tipo, Collection<String> ids) {
        List<ObjectId> objectIds = ids.stream().filter(ObjectId::isValid).map(ObjectId::new).toList();
        Map<String, Referencia> encontradas = new HashMap<>();
        if (objectIds.isEmpty()) {
            return encontradas;
        }
        boolean esServicio = tipo == TipoOferta.Servicio;
        String nombre = esServicio ? "nombreComercial" : "nombre";
        mongo.getCollection(esServicio ? "servicios" : "atractivos")
                .find(Filters.in("_id", objectIds))
                .projection(Projections.include(nombre, "categoria", "imagenes", "estadoPublicacion", "prestadorActivo"))
                .forEach(documento -> encontradas.put(documento.getObjectId("_id").toHexString(), new Referencia(
                        documento.getString(nombre), documento.getString("categoria"), portada(documento),
                        !esServicio || visible(documento))));
        return encontradas;
    }

    private static boolean visible(Document servicio) {
        return EstadoPublicacion.Publicado.name().equals(servicio.getString("estadoPublicacion"))
                && Boolean.TRUE.equals(servicio.getBoolean("prestadorActivo"));
    }

    /** La primera imagen es la portada (RF48), la misma de las tarjetas de WF-05. */
    private static String portada(Document documento) {
        List<String> imagenes = documento.getList("imagenes", String.class);
        return imagenes == null || imagenes.isEmpty() ? null : imagenes.get(0);
    }
}
