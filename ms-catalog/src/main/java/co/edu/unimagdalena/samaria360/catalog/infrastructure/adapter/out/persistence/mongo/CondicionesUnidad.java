package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.domain.model.FiltrosAlojamiento;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Condiciones que debe cumplir <b>una misma unidad</b> de un alojamiento (D1): capacidad, tipo de habitación y precio
 * máximo, las mismas reglas que {@link FiltrosAlojamiento#cumple}. Se construyen una sola vez y se escriben de dos
 * formas: como consulta para el {@code $elemMatch} del filtro y como expresión para el {@code $filter} del precio
 * «desde». Así el precio de la tarjeta siempre sale de unidades que cumplen el filtro.
 */
final class CondicionesUnidad {

    private enum Operador { MAYOR_O_IGUAL, MENOR_O_IGUAL, TEXTO_EXACTO }

    private record Condicion(String campo, Operador operador, Object valor) {
    }

    private final List<Condicion> condiciones;

    private CondicionesUnidad(List<Condicion> condiciones) {
        this.condiciones = List.copyOf(condiciones);
    }

    /** Huéspedes, tipo de habitación y, si hay, el precio máximo de la búsqueda. */
    static CondicionesUnidad de(FiltrosAlojamiento filtros, BigDecimal precioMaximo) {
        List<Condicion> condiciones = new ArrayList<>();
        if (filtros.huespedes() != null) {
            condiciones.add(new Condicion("capacidadMaxHuespedes", Operador.MAYOR_O_IGUAL, filtros.huespedes()));
        }
        if (filtros.tipoHabitacion() != null) {
            condiciones.add(new Condicion("tipoHabitacion", Operador.TEXTO_EXACTO,
                    "^" + OfertaMongoAdapter.escaparRegex(filtros.tipoHabitacion()) + "\\z"));
        }
        if (precioMaximo != null) {
            condiciones.add(new Condicion("precioNoche", Operador.MENOR_O_IGUAL, new Decimal128(precioMaximo)));
        }
        return new CondicionesUnidad(condiciones);
    }

    /** Consulta para {@code detalleCategoria.unidades: {$elemMatch: …}}. */
    Document consulta() {
        Document consulta = new Document();
        for (Condicion condicion : condiciones) {
            consulta.append(condicion.campo(), switch (condicion.operador()) {
                case MAYOR_O_IGUAL -> new Document("$gte", condicion.valor());
                case MENOR_O_IGUAL -> new Document("$lte", condicion.valor());
                case TEXTO_EXACTO -> new Document("$regex", condicion.valor()).append("$options", "i");
            });
        }
        return consulta;
    }

    /** Expresión de agregación sobre la variable {@code $$<variable>} (una unidad), para el {@code $filter}. */
    Document expresion(String variable) {
        List<Document> partes = new ArrayList<>();
        for (Condicion condicion : condiciones) {
            String campo = "$$" + variable + "." + condicion.campo();
            partes.add(switch (condicion.operador()) {
                case MAYOR_O_IGUAL -> new Document("$gte", List.of(campo, condicion.valor()));
                case MENOR_O_IGUAL -> new Document("$lte", List.of(campo, condicion.valor()));
                case TEXTO_EXACTO -> new Document("$regexMatch", new Document("input", campo)
                        .append("regex", condicion.valor()).append("options", "i"));
            });
        }
        return new Document("$and", partes);
    }
}
