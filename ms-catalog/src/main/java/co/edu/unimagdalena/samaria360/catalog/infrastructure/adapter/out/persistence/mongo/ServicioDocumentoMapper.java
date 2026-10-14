package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.domain.model.Categoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CondicionAccesibilidad;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Contacto;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Coordenada;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleAlojamiento;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleAlquilerEquipo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleCategoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleGastronomia;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleTour;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoPublicacion;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;
import co.edu.unimagdalena.samaria360.catalog.domain.model.UnidadAlojamiento;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Documento de {@code servicios} (esquema v2, migración M001) → {@link ServicioComercial}. Mapeo explícito: las
 * coordenadas GeoJSON van en orden [longitud, latitud] y {@code idUnidad} es un ObjectId que el dominio recibe como
 * texto.
 */
final class ServicioDocumentoMapper {

    private static final Set<String> ACCESIBILIDAD_CONOCIDA = Set.of("MovilidadReducida", "Visual", "Auditiva");

    private ServicioDocumentoMapper() {
    }

    static ServicioComercial aDominio(Document documento) {
        Categoria categoria = Categoria.valueOf(documento.getString("categoria"));
        Document contacto = documento.get("contacto", Document.class);
        List<?> coordenadas = documento.get("coordenadas", Document.class).get("coordinates", List.class);
        return new ServicioComercial(
                documento.getObjectId("_id").toHexString(),
                UUID.fromString(documento.getString("prestadorId")),
                documento.getString("nombreComercial"),
                categoria,
                detalle(categoria, documento.get("detalleCategoria", Document.class)),
                documento.getString("descripcion"),
                new Coordenada(numero(coordenadas.get(0)).doubleValue(), numero(coordenadas.get(1)).doubleValue()),
                contacto == null ? null : new Contacto(contacto.getString("telefono"), contacto.getString("correo")),
                EstadoPublicacion.valueOf(documento.getString("estadoPublicacion")),
                Boolean.TRUE.equals(documento.getBoolean("prestadorActivo")),
                accesibilidad(textos(documento, "condicionesAccesibilidad")),
                documento.getString("horarioAtencion"),
                decimal(documento.get("precioBase")),
                textos(documento, "imagenes"));
    }

    private static DetalleCategoria detalle(Categoria categoria, Document detalle) {
        return switch (categoria) {
            case Alojamiento -> new DetalleAlojamiento(
                    detalle.getList("unidades", Document.class).stream().map(ServicioDocumentoMapper::unidad).toList(),
                    textos(detalle, "serviciosIncluidos"));
            case Gastronomia -> new DetalleGastronomia(detalle.getString("tipoCocina"),
                    numero(detalle.get("aforoMaxTurno")).intValue(), detalle.getBoolean("menuVegetariano"),
                    detalle.getString("rangoPrecioPlato"));
            case Tour -> new DetalleTour(detalle.getString("puntoEncuentro"), decimal(detalle.get("duracionHoras")),
                    detalle.getString("nivelDificultad"), numero(detalle.get("aforoMaxGrupo")).intValue());
            case AlquilerEquipo -> new DetalleAlquilerEquipo(detalle.getString("tipoVehiculoEquipo"),
                    numero(detalle.get("unidadesDisponibles")).intValue(), detalle.getString("condicionesAlquiler"));
        };
    }

    private static UnidadAlojamiento unidad(Document unidad) {
        return new UnidadAlojamiento(unidad.get("idUnidad", ObjectId.class).toHexString(), unidad.getString("nombre"),
                unidad.getString("tipoHabitacion"), numero(unidad.get("numHabitaciones")).intValue(),
                numero(unidad.get("capacidadMaxHuespedes")).intValue(), decimal(unidad.get("precioNoche")));
    }

    /** El esquema admite texto libre (pendiente para el SAD): los valores que no son del SRS se ignoran. */
    static Set<CondicionAccesibilidad> accesibilidad(List<String> valores) {
        if (valores == null) {
            return Set.of();
        }
        return valores.stream().filter(ACCESIBILIDAD_CONOCIDA::contains).map(CondicionAccesibilidad::desdeTexto)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static List<String> textos(Document documento, String campo) {
        List<?> lista = documento.get(campo, List.class);
        return lista == null ? List.of()
                : lista.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    private static Number numero(Object valor) {
        return (Number) valor;
    }

    private static BigDecimal decimal(Object valor) {
        if (valor == null) {
            return null;
        }
        return valor instanceof Decimal128 decimal ? decimal.bigDecimalValue() : new BigDecimal(valor.toString());
    }
}
