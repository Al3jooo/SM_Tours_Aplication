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
import co.edu.unimagdalena.samaria360.catalog.domain.model.DiaSemana;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoPublicacion;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Oferta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Revision;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;
import co.edu.unimagdalena.samaria360.catalog.domain.model.UnidadAlojamiento;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Documento de {@code servicios} (esquema v3, migración M008) ↔ {@link ServicioComercial}. Mapeo explícito: las
 * coordenadas GeoJSON van en orden [longitud, latitud], {@code idUnidad} es un ObjectId que el dominio recibe como
 * texto, los enteros van como int32, el dinero y las horas como Decimal128 y las salidas de la oferta como {@code HH:mm}.
 * Los campos opcionales que faltan no se escriben.
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
                textos(documento, "imagenes"),
                documento.getString("direccion"),
                oferta(documento.get("oferta", Document.class)),
                documento.getString("rnt"),
                instante(documento.getDate("fechaRegistro")),
                revision(documento));
    }

    private static Oferta oferta(Document oferta) {
        if (oferta == null) {
            return null;
        }
        List<DiaSemana> dias = textos(oferta, "diasOperacion").stream().map(DiaSemana::valueOf).toList();
        List<LocalTime> salidas = textos(oferta, "salidas").stream().map(LocalTime::parse).toList();
        return new Oferta(dias, salidas);
    }

    /** Un Publicado de la semilla no tiene revisión; uno revisado tiene revisadoPor y fechaRevision. */
    private static Revision revision(Document documento) {
        String revisadoPor = documento.getString("revisadoPor");
        if (revisadoPor == null) {
            return null;
        }
        return new Revision(UUID.fromString(revisadoPor), documento.getDate("fechaRevision").toInstant(),
                documento.getString("motivoRechazo"));
    }

    static Document aDocumento(ServicioComercial servicio) {
        Document documento = new Document("_id", new ObjectId(servicio.getId().orElseThrow()))
                .append("prestadorId", servicio.getPrestadorId().toString())
                .append("nombreComercial", servicio.getNombreComercial())
                .append("categoria", servicio.getCategoria().name());
        servicio.getDescripcion().ifPresent(descripcion -> documento.append("descripcion", descripcion));
        servicio.getDireccion().ifPresent(direccion -> documento.append("direccion", direccion));
        documento.append("coordenadas", new Document("type", "Point").append("coordinates",
                List.of(servicio.getCoordenada().longitud(), servicio.getCoordenada().latitud())));
        servicio.getContacto().ifPresent(contacto -> documento.append("contacto",
                new Document("telefono", contacto.telefono()).append("correo", contacto.correo())));
        documento.append("estadoPublicacion", servicio.getEstadoPublicacion().name())
                .append("prestadorActivo", servicio.isPrestadorActivo());
        servicio.getRnt().ifPresent(rnt -> documento.append("rnt", rnt));
        documento.append("condicionesAccesibilidad", servicio.getCondicionesAccesibilidad().stream().sorted()
                .map(CondicionAccesibilidad::name).toList());
        servicio.getHorarioAtencion().ifPresent(horario -> documento.append("horarioAtencion", horario));
        servicio.getPrecioBase().ifPresent(precio -> documento.append("precioBase", new Decimal128(precio)));
        documento.append("imagenes", servicio.getImagenes())
                .append("detalleCategoria", detalle(servicio.getDetalle()));
        servicio.getOferta().ifPresent(oferta -> documento.append("oferta", oferta(oferta)));
        servicio.getFechaRegistro().ifPresent(fecha -> documento.append("fechaRegistro", Date.from(fecha)));
        servicio.getRevision().ifPresent(revision -> revision(documento, revision));
        return documento;
    }

    /** Los campos de la revisión, para el insert y para el UPDATE condicional de la aprobación. */
    static Document revision(Document documento, Revision revision) {
        documento.append("revisadoPor", revision.revisadoPor().toString())
                .append("fechaRevision", Date.from(revision.fechaRevision()));
        if (revision.motivoRechazo() != null) {
            documento.append("motivoRechazo", revision.motivoRechazo());
        }
        return documento;
    }

    private static Document detalle(DetalleCategoria detalle) {
        Document documento = new Document();
        if (detalle instanceof DetalleAlojamiento alojamiento) {
            List<Document> unidades = new ArrayList<>();
            for (UnidadAlojamiento unidad : alojamiento.unidades()) {
                unidades.add(new Document("idUnidad", new ObjectId(unidad.idUnidad()))
                        .append("nombre", unidad.nombre())
                        .append("tipoHabitacion", unidad.tipoHabitacion())
                        .append("numHabitaciones", unidad.numHabitaciones())
                        .append("capacidadMaxHuespedes", unidad.capacidadMaxHuespedes())
                        .append("precioNoche", new Decimal128(unidad.precioNoche())));
            }
            return documento.append("unidades", unidades)
                    .append("serviciosIncluidos", alojamiento.serviciosIncluidos());
        }
        if (detalle instanceof DetalleGastronomia gastronomia) {
            documento.append("tipoCocina", gastronomia.tipoCocina())
                    .append("aforoMaxTurno", gastronomia.aforoMaxTurno());
            if (gastronomia.menuVegetariano() != null) {
                documento.append("menuVegetariano", gastronomia.menuVegetariano());
            }
            return documento.append("rangoPrecioPlato", gastronomia.rangoPrecioPlato());
        }
        if (detalle instanceof DetalleTour tour) {
            documento.append("puntoEncuentro", tour.puntoEncuentro())
                    .append("duracionHoras", new Decimal128(tour.duracionHoras()));
            if (tour.nivelDificultad() != null) {
                documento.append("nivelDificultad", tour.nivelDificultad());
            }
            return documento.append("aforoMaxGrupo", tour.aforoMaxGrupo());
        }
        DetalleAlquilerEquipo alquiler = (DetalleAlquilerEquipo) detalle;
        documento.append("tipoVehiculoEquipo", alquiler.tipoVehiculoEquipo())
                .append("unidadesDisponibles", alquiler.unidadesDisponibles());
        if (alquiler.condicionesAlquiler() != null) {
            documento.append("condicionesAlquiler", alquiler.condicionesAlquiler());
        }
        return documento;
    }

    private static Document oferta(Oferta oferta) {
        Document documento = new Document("diasOperacion",
                oferta.diasOperacion().stream().map(DiaSemana::name).toList());
        if (!oferta.salidas().isEmpty()) {
            documento.append("salidas", oferta.salidas().stream().map(LocalTime::toString).toList());
        }
        return documento;
    }

    private static Instant instante(Date fecha) {
        return fecha == null ? null : fecha.toInstant();
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
