package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.CuentaRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Cuenta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.RolCuenta;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Colección {@code cuentas}: {@code {_id: uuid, rol, estadoCuenta, version: long, actualizadoEn}} (M003) y {@code rnt}
 * solo en las cuentas de un prestador (M008, DEC-146).
 */
@Component
public class CuentaMongoAdapter implements CuentaRepositoryPort {

    private static final String CUENTAS = "cuentas";

    private final MongoTemplate mongo;

    public CuentaMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<Cuenta> buscar(UUID id) {
        Document documento = mongo.getCollection(CUENTAS).find(new Document("_id", id.toString())).first();
        if (documento == null) {
            return Optional.empty();
        }
        return Optional.of(new Cuenta(id, RolCuenta.valueOf(documento.getString("rol")),
                EstadoCuenta.valueOf(documento.getString("estadoCuenta")), documento.getLong("version"),
                documento.getString("rnt")));
    }

    @Override
    public OptionalLong versionDe(UUID id) {
        Document documento = mongo.getCollection(CUENTAS).find(new Document("_id", id.toString()))
                .projection(new Document("version", 1)).first();
        return documento == null ? OptionalLong.empty() : OptionalLong.of(documento.getLong("version"));
    }

    /**
     * {@code updateOne({_id, version: {$lt: v}}, $set, upsert)}: si hay una versión igual o mayor el filtro no coincide y el
     * upsert intenta insertar el mismo {@code _id}, lo que MongoDB rechaza con una clave duplicada (E11000): es el
     * «ya había una versión igual o mayor».
     */
    @Override
    public boolean guardarSiEsMasNueva(Cuenta cuenta) {
        Query filtro = Query.query(Criteria.where("_id").is(cuenta.id().toString())
                .and("version").lt(cuenta.version()));
        Update cambio = new Update()
                .set("rol", cuenta.rol().name())
                .set("estadoCuenta", cuenta.estado().name())
                .set("version", cuenta.version())
                .set("actualizadoEn", new Date());
        // Sin RNT (otro rol, o un evento anterior a V17 de ms-auth), el campo no queda con un valor viejo.
        if (cuenta.rnt() != null) {
            cambio.set("rnt", cuenta.rnt());
        } else {
            cambio.unset("rnt");
        }
        try {
            mongo.upsert(filtro, cambio, CUENTAS);
            return true;
        } catch (DuplicateKeyException masNueva) {
            return false;
        }
    }
}
