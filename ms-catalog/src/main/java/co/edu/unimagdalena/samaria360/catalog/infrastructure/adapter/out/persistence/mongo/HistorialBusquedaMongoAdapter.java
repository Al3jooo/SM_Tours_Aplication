package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.HistorialBusquedaRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BusquedaReciente;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Categoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CondicionAccesibilidad;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CriteriosGuardados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.OrdenBusqueda;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ParametrosFiltrosCategoria;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Colección {@code historial_busqueda}: {@code {_id, turistaId, criteriosBusqueda, fecha, clave}} (Entregable 2, §2.4,
 * más {@code clave}; M007). {@code criteriosBusqueda} es el JSON canónico de los criterios normalizados, con los nombres
 * de los parámetros de {@code GET /oferta} y en un orden fijo; los campos sin valor no se escriben.
 */
@Component
public class HistorialBusquedaMongoAdapter implements HistorialBusquedaRepositoryPort {

    private static final String HISTORIAL = "historial_busqueda";

    private final MongoTemplate mongo;
    private final ObjectMapper json;

    public HistorialBusquedaMongoAdapter(MongoTemplate mongo, ObjectMapper json) {
        this.mongo = mongo;
        this.json = json;
    }

    @Override
    public boolean guardar(UUID turistaId, CriteriosGuardados criterios, Instant fecha) {
        try {
            coleccion().updateOne(clave(turistaId, criterios), cambios(criterios, fecha),
                    new UpdateOptions().upsert(true));
            return true;
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public boolean actualizar(UUID turistaId, CriteriosGuardados criterios, Instant fecha) {
        try {
            return coleccion().updateOne(clave(turistaId, criterios), cambios(criterios, fecha)).getMatchedCount() > 0;
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public void podar(UUID turistaId, int conservar) {
        List<ObjectId> sobrantes = new ArrayList<>();
        coleccion().find(Filters.eq("turistaId", turistaId.toString())).sort(recientePrimero()).skip(conservar)
                .projection(Projections.include("_id"))
                .forEach(documento -> sobrantes.add(documento.getObjectId("_id")));
        if (!sobrantes.isEmpty()) {
            coleccion().deleteMany(Filters.in("_id", sobrantes));
        }
    }

    @Override
    public List<BusquedaReciente> listar(UUID turistaId, int maximo) {
        List<BusquedaReciente> busquedas = new ArrayList<>();
        coleccion().find(Filters.eq("turistaId", turistaId.toString())).sort(recientePrimero()).limit(maximo)
                .forEach(documento -> busquedas.add(new BusquedaReciente(documento.getObjectId("_id").toHexString(),
                        leer(documento.getString("criteriosBusqueda")), documento.getDate("fecha").toInstant())));
        return busquedas;
    }

    private MongoCollection<Document> coleccion() {
        return mongo.getCollection(HISTORIAL);
    }

    /** Las mismas claves del índice único {@code historial_turista_clave}. */
    private static Bson clave(UUID turistaId, CriteriosGuardados criterios) {
        return Filters.and(Filters.eq("turistaId", turistaId.toString()), Filters.eq("clave", criterios.clave()));
    }

    /** El texto se guarda tal como se escribió la última vez; la clave no distingue mayúsculas. */
    private Bson cambios(CriteriosGuardados criterios, Instant fecha) {
        return Updates.combine(Updates.set("criteriosBusqueda", escribir(criterios)),
                Updates.set("fecha", Date.from(fecha)));
    }

    private static Document recientePrimero() {
        return new Document("fecha", -1).append("_id", -1);
    }

    private String escribir(CriteriosGuardados c) {
        ParametrosFiltrosCategoria f = c.masFiltros();
        Map<String, Object> campos = new LinkedHashMap<>();
        poner(campos, "texto", c.texto());
        poner(campos, "categoria", c.categoria());
        poner(campos, "precioMax", c.precioMax());
        poner(campos, "usarUbicacion", c.usarUbicacion() ? Boolean.TRUE : null);
        poner(campos, "distanciaKm", c.distanciaKm());
        poner(campos, "accesibilidad", c.accesibilidad().isEmpty() ? null : c.accesibilidad());
        poner(campos, "orden", c.orden());
        poner(campos, "serviciosIncluidos", f.serviciosIncluidos().isEmpty() ? null : f.serviciosIncluidos());
        poner(campos, "huespedes", f.huespedes());
        poner(campos, "tipoHabitacion", f.tipoHabitacion());
        poner(campos, "menuVegetariano", f.menuVegetariano());
        poner(campos, "tipoCocina", f.tipoCocina());
        poner(campos, "nivelDificultad", f.nivelDificultad());
        poner(campos, "duracionMaxHoras", f.duracionMaxHoras());
        poner(campos, "tipoVehiculoEquipo", f.tipoVehiculoEquipo());
        try {
            return json.writeValueAsString(campos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron serializar los criterios", e);
        }
    }

    private CriteriosGuardados leer(String texto) {
        JsonNode n;
        try {
            n = json.readTree(texto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("criteriosBusqueda no es un JSON válido", e);
        }
        List<String> servicios = new ArrayList<>();
        n.path("serviciosIncluidos").forEach(valor -> servicios.add(valor.asText()));
        List<CondicionAccesibilidad> accesibilidad = new ArrayList<>();
        n.path("accesibilidad").forEach(valor -> accesibilidad.add(CondicionAccesibilidad.valueOf(valor.asText())));
        ParametrosFiltrosCategoria masFiltros = new ParametrosFiltrosCategoria(servicios,
                valor(n, "huespedes", JsonNode::asInt), valor(n, "tipoHabitacion", JsonNode::asText),
                valor(n, "menuVegetariano", JsonNode::asBoolean), valor(n, "tipoCocina", JsonNode::asText),
                valor(n, "nivelDificultad", JsonNode::asText), valor(n, "duracionMaxHoras", nodo -> new BigDecimal(nodo.asText())),
                valor(n, "tipoVehiculoEquipo", JsonNode::asText));
        return new CriteriosGuardados(valor(n, "texto", JsonNode::asText),
                valor(n, "categoria", nodo -> Categoria.valueOf(nodo.asText())),
                valor(n, "precioMax", nodo -> new BigDecimal(nodo.asText())),
                n.path("usarUbicacion").asBoolean(false),
                valor(n, "distanciaKm", JsonNode::asDouble), accesibilidad,
                valor(n, "orden", nodo -> OrdenBusqueda.valueOf(nodo.asText())), masFiltros);
    }

    private static void poner(Map<String, Object> campos, String nombre, Object valor) {
        if (valor != null) {
            campos.put(nombre, valor instanceof Enum<?> e ? e.name() : valor);
        }
    }

    private static <T> T valor(JsonNode nodo, String campo, Function<JsonNode, T> leer) {
        JsonNode valor = nodo.get(campo);
        return valor == null || valor.isNull() ? null : leer.apply(valor);
    }
}
