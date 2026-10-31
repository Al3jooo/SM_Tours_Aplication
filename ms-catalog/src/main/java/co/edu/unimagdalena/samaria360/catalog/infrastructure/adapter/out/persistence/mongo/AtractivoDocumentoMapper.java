package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CondicionAccesibilidad;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Coordenada;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoOperativo;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Documento de la colección {@code atractivos} (validador v2, M012) ↔ {@link AtractivoPublico}. Las coordenadas
 * GeoJSON van en orden [longitud, latitud]; las condiciones de accesibilidad, en el orden de RF27. Los campos
 * opcionales sin valor no se escriben (y en una edición se quitan con {@code $unset}).
 */
final class AtractivoDocumentoMapper {

    private AtractivoDocumentoMapper() {
    }

    static AtractivoPublico aDominio(Document documento) {
        List<?> coordenadas = documento.get("coordenadas", Document.class).get("coordinates", List.class);
        String estado = documento.getString("estadoOperativo");
        Object version = documento.get("version");
        return new AtractivoPublico(documento.getObjectId("_id").toHexString(),
                UUID.fromString(documento.getString("gestorId")), documento.getString("nombre"),
                new Coordenada(((Number) coordenadas.get(0)).doubleValue(), ((Number) coordenadas.get(1)).doubleValue()),
                documento.getString("descripcion"), documento.getString("categoria"),
                documento.getList("imagenes", String.class), ((Number) documento.get("limiteCapacidadCarga")).intValue(),
                estado == null ? null : EstadoOperativo.valueOf(estado), documento.getString("normasAmbientales"),
                ServicioDocumentoMapper.accesibilidad(documento.getList("condicionesAccesibilidad", String.class)),
                documento.getString("horarioAtencion"), instante(documento.getDate("fechaRegistro")),
                instante(documento.getDate("fechaActualizacion")),
                version == null ? 0 : ((Number) version).intValue());
    }

    static Document aDocumento(AtractivoPublico atractivo) {
        Document documento = new Document("_id", new ObjectId(atractivo.getId().orElseThrow()))
                .append("gestorId", atractivo.getGestorId().toString());
        documento.putAll(datos(atractivo));
        documento.append("imagenes", atractivo.getImagenes());
        atractivo.getEstadoOperativo().ifPresent(estado -> documento.append("estadoOperativo", estado.name()));
        atractivo.getFechaRegistro().ifPresent(fecha -> documento.append("fechaRegistro", Date.from(fecha)));
        documento.append("version", atractivo.getVersion());
        return documento;
    }

    /** Los campos editables con valor (para {@code $set}); los opcionales vacíos van en {@link #vacios}. */
    static Document datos(AtractivoPublico atractivo) {
        Coordenada coordenada = atractivo.getCoordenada();
        Document datos = new Document("nombre", atractivo.getNombre())
                .append("coordenadas", new Document("type", "Point")
                        .append("coordinates", List.of(coordenada.longitud(), coordenada.latitud())));
        atractivo.getDescripcion().ifPresent(descripcion -> datos.append("descripcion", descripcion));
        atractivo.getCategoria().ifPresent(categoria -> datos.append("categoria", categoria));
        datos.append("limiteCapacidadCarga", atractivo.getLimiteCapacidadCarga());
        atractivo.getNormasAmbientales().ifPresent(normas -> datos.append("normasAmbientales", normas));
        atractivo.getHorarioAtencion().ifPresent(horario -> datos.append("horarioAtencion", horario));
        datos.append("condicionesAccesibilidad", atractivo.getCondicionesAccesibilidad().stream().sorted()
                .map(CondicionAccesibilidad::name).toList());
        return datos;
    }

    /** Los opcionales editables sin valor (para {@code $unset}). */
    static Document vacios(AtractivoPublico atractivo) {
        Document vacios = new Document();
        if (atractivo.getNormasAmbientales().isEmpty()) {
            vacios.append("normasAmbientales", "");
        }
        if (atractivo.getHorarioAtencion().isEmpty()) {
            vacios.append("horarioAtencion", "");
        }
        return vacios;
    }

    private static Instant instante(Date fecha) {
        return fecha == null ? null : fecha.toInstant();
    }
}
