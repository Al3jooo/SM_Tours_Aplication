package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.EventoRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EventoTuristico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Colección {@code eventos} (M013): {@code {_id, gestorId, nombre, fechaInicio, fechaFin, ubicacion, descripcion,
 * fechaRegistro}}. Las fechas de día se guardan como {@code date} a las 00:00 de Bogotá (DEC-215). La agenda usa el
 * índice {@code eventos_agenda} y el panel del Gestor, {@code eventos_registro}.
 */
@Component
public class EventoMongoAdapter implements EventoRepositoryPort {

    private static final String EVENTOS = "eventos";
    private static final Document RECIENTES_PRIMERO = new Document("fechaRegistro", -1).append("_id", -1);
    private static final Document PROXIMOS_PRIMERO = new Document("fechaInicio", 1).append("_id", 1);

    private final MongoTemplate mongo;

    public EventoMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public String nuevoId() {
        return new ObjectId().toHexString();
    }

    @Override
    public void insertar(EventoTuristico evento) {
        coleccion().insertOne(new Document("_id", new ObjectId(evento.id()))
                .append("gestorId", evento.gestorId().toString())
                .append("nombre", evento.nombre())
                .append("fechaInicio", fecha(evento.fechaInicio()))
                .append("fechaFin", fecha(evento.fechaFin()))
                .append("ubicacion", evento.ubicacion())
                .append("descripcion", evento.descripcion())
                .append("fechaRegistro", Date.from(evento.fechaRegistro())));
    }

    @Override
    public PaginaResultados<EventoTuristico> listar(int pagina, int tamano) {
        return pagina(new Document(), RECIENTES_PRIMERO, pagina, tamano);
    }

    @Override
    public PaginaResultados<EventoTuristico> vigentes(LocalDate desde, int pagina, int tamano) {
        return pagina(Filters.gte("fechaFin", fecha(desde)), PROXIMOS_PRIMERO, pagina, tamano);
    }

    /** Una página fuera de rango llega vacía, con el total real. */
    private PaginaResultados<EventoTuristico> pagina(Bson filtro, Document orden, int pagina, int tamano) {
        long total = coleccion().countDocuments(filtro);
        long salto = (long) pagina * tamano;
        List<EventoTuristico> eventos = new ArrayList<>();
        if (salto < total) {
            coleccion().find(filtro).sort(orden).skip((int) salto).limit(tamano)
                    .forEach(documento -> eventos.add(aDominio(documento)));
        }
        return new PaginaResultados<>(eventos, pagina, tamano, total);
    }

    private MongoCollection<Document> coleccion() {
        return mongo.getCollection(EVENTOS);
    }

    private static Date fecha(LocalDate dia) {
        return Date.from(dia.atStartOfDay(EventoTuristico.ZONA).toInstant());
    }

    /** Se lee el día en Bogotá: una hora distinta de las 00:00 (p. ej. de un script) no cambia el día. */
    private static LocalDate dia(Date fecha) {
        return LocalDate.ofInstant(fecha.toInstant(), EventoTuristico.ZONA);
    }

    private static EventoTuristico aDominio(Document documento) {
        return new EventoTuristico(documento.getObjectId("_id").toHexString(),
                UUID.fromString(documento.getString("gestorId")), documento.getString("nombre"),
                dia(documento.getDate("fechaInicio")), dia(documento.getDate("fechaFin")),
                documento.getString("ubicacion"), documento.getString("descripcion"),
                documento.getDate("fechaRegistro").toInstant());
    }
}
