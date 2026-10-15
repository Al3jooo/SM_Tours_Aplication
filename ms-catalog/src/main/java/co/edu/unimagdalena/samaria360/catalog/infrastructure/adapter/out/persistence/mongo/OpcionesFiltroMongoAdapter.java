package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.OpcionesFiltroRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Categoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.OpcionesFiltro;
import co.edu.unimagdalena.samaria360.catalog.domain.model.OpcionesFiltro.Rango;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioIncluido;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Opciones de «Más filtros»: una agregación {@code $match → $group} sobre los servicios visibles de la categoría
 * (los mismos que puede devolver la búsqueda pública, RF16 y RF22), así cada opción tiene al menos un resultado.
 */
@Component
public class OpcionesFiltroMongoAdapter implements OpcionesFiltroRepositoryPort {

    private static final String SERVICIOS = "servicios";
    private static final String DETALLE = "$detalleCategoria.";
    private static final String UNIDADES = DETALLE + "unidades.";

    private final MongoTemplate mongo;

    public OpcionesFiltroMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public OpcionesFiltro opciones(Categoria categoria) {
        Document visibles = new Document("estadoPublicacion", "Publicado")
                .append("prestadorActivo", true)
                .append("categoria", categoria.name());
        Aggregation agregacion = Aggregation.newAggregation(
                etapa("$match", visibles),
                etapa("$group", grupo(categoria)));
        Document resultado = mongo.aggregate(agregacion, SERVICIOS, Document.class).getUniqueMappedResult();
        Document valores = resultado == null ? new Document() : resultado;

        return switch (categoria) {
            case Alojamiento -> OpcionesFiltro.alojamiento(serviciosIncluidos(valores),
                    textosDeListas(valores, "tiposHabitacion"),
                    rangoEntero(valores, "huespedesMinimo", "huespedesMaximo"));
            case Gastronomia -> OpcionesFiltro.gastronomia(Boolean.TRUE.equals(valores.getBoolean("opcionVegetariana")),
                    textos(valores, "tiposCocina"));
            case Tour -> OpcionesFiltro.tour(textos(valores, "nivelesDificultad"),
                    rangoDecimal(valores, "duracionMinima", "duracionMaxima"));
            case AlquilerEquipo -> OpcionesFiltro.alquilerEquipo(textos(valores, "tiposVehiculoEquipo"));
        };
    }

    /**
     * $addToSet omite los campos ausentes (p. ej. un tour sin nivelDificultad); $min y $max los ignoran. En
     * Alojamiento, los tipos de habitación y los huéspedes salen de todas las unidades (D1): se juntan las listas de
     * cada alojamiento y el mínimo y el máximo se calculan sobre los de sus unidades.
     */
    private static Document grupo(Categoria categoria) {
        Document grupo = new Document("_id", null);
        switch (categoria) {
            case Alojamiento -> grupo
                    .append("serviciosIncluidos", new Document("$push", DETALLE + "serviciosIncluidos"))
                    .append("tiposHabitacion", new Document("$push", UNIDADES + "tipoHabitacion"))
                    .append("huespedesMinimo", new Document("$min",
                            new Document("$min", UNIDADES + "capacidadMaxHuespedes")))
                    .append("huespedesMaximo", new Document("$max",
                            new Document("$max", UNIDADES + "capacidadMaxHuespedes")));
            case Gastronomia -> grupo
                    .append("opcionVegetariana", new Document("$max",
                            new Document("$eq", Arrays.asList(DETALLE + "menuVegetariano", true))))
                    .append("tiposCocina", new Document("$addToSet", DETALLE + "tipoCocina"));
            case Tour -> grupo
                    .append("nivelesDificultad", new Document("$addToSet", DETALLE + "nivelDificultad"))
                    .append("duracionMinima", new Document("$min", DETALLE + "duracionHoras"))
                    .append("duracionMaxima", new Document("$max", DETALLE + "duracionHoras"));
            case AlquilerEquipo -> grupo
                    .append("tiposVehiculoEquipo", new Document("$addToSet", DETALLE + "tipoVehiculoEquipo"));
        }
        return grupo;
    }

    /** Une las listas de cada alojamiento; ignora valores fuera del vocabulario (datos anteriores al enum). */
    private static Set<ServicioIncluido> serviciosIncluidos(Document valores) {
        Set<ServicioIncluido> servicios = EnumSet.noneOf(ServicioIncluido.class);
        List<?> listas = valores.get("serviciosIncluidos", List.class);
        if (listas == null) {
            return servicios;
        }
        listas.stream().filter(List.class::isInstance).flatMap(lista -> ((List<?>) lista).stream())
                .filter(String.class::isInstance).map(String.class::cast)
                .forEach(texto -> Arrays.stream(ServicioIncluido.values())
                        .filter(servicio -> servicio.name().equals(texto))
                        .forEach(servicios::add));
        return servicios;
    }

    /** Une las listas de cada alojamiento (p. ej. los tipos de habitación de sus unidades). */
    private static List<String> textosDeListas(Document valores, String campo) {
        List<?> listas = valores.get(campo, List.class);
        return listas == null ? List.of()
                : listas.stream().filter(List.class::isInstance).flatMap(lista -> ((List<?>) lista).stream())
                .filter(String.class::isInstance).map(String.class::cast)
                .filter(texto -> !texto.isBlank()).toList();
    }

    private static List<String> textos(Document valores, String campo) {
        List<?> lista = valores.get(campo, List.class);
        return lista == null ? List.of()
                : lista.stream().filter(String.class::isInstance).map(String.class::cast)
                .filter(texto -> !texto.isBlank()).toList();
    }

    private static Rango<Integer> rangoEntero(Document valores, String minimo, String maximo) {
        Number desde = valores.get(minimo, Number.class);
        Number hasta = valores.get(maximo, Number.class);
        return desde == null || hasta == null ? null : new Rango<>(desde.intValue(), hasta.intValue());
    }

    private static Rango<BigDecimal> rangoDecimal(Document valores, String minimo, String maximo) {
        BigDecimal desde = decimal(valores.get(minimo));
        BigDecimal hasta = decimal(valores.get(maximo));
        return desde == null || hasta == null ? null : new Rango<>(desde, hasta);
    }

    private static BigDecimal decimal(Object valor) {
        if (valor instanceof Decimal128 decimal) {
            return decimal.bigDecimalValue();
        }
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        return valor instanceof Number numero ? new BigDecimal(numero.toString()) : null;
    }

    private static AggregationOperation etapa(String nombre, Document contenido) {
        Document etapa = new Document(nombre, Objects.requireNonNull(contenido));
        return contexto -> etapa;
    }
}
