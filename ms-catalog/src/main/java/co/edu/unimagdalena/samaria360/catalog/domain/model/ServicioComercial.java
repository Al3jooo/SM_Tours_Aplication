package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio comercial de un prestador (RF09, RF10, RF37). Inmutable: los cambios de estado los hará la
 * administración en iteraciones posteriores.
 */
public final class ServicioComercial {

    public static final int LONGITUD_MAXIMA_NOMBRE = 150;

    private final String id;
    private final UUID prestadorId;
    private final String nombreComercial;
    private final Categoria categoria;
    private final DetalleCategoria detalle;
    private final String descripcion;
    private final Coordenada coordenada;
    private final Contacto contacto;
    private final EstadoPublicacion estadoPublicacion;
    private final boolean prestadorActivo;
    private final Set<CondicionAccesibilidad> condicionesAccesibilidad;
    private final String horarioAtencion;
    private final BigDecimal precioBase;
    private final List<String> imagenes;

    /**
     * @param id nulo antes de persistir
     * @param prestadorActivo copia desnormalizada del estado de la cuenta del prestador en ms-auth (RF22)
     * @param precioBase precio «desde» (RF37). En Alojamiento se deriva de las unidades (el menor precio por noche):
     *                   puede llegar nulo, y si llega debe coincidir, como exige el {@code $expr} del esquema v2
     */
    public ServicioComercial(String id, UUID prestadorId, String nombreComercial, Categoria categoria,
                             DetalleCategoria detalle, String descripcion, Coordenada coordenada, Contacto contacto,
                             EstadoPublicacion estadoPublicacion, boolean prestadorActivo,
                             Set<CondicionAccesibilidad> condicionesAccesibilidad, String horarioAtencion,
                             BigDecimal precioBase, List<String> imagenes) {
        this.id = id;
        this.prestadorId = Objects.requireNonNull(prestadorId, "prestadorId");
        this.nombreComercial = Validaciones.textoMaximo(nombreComercial, LONGITUD_MAXIMA_NOMBRE, "nombreComercial");
        this.categoria = Objects.requireNonNull(categoria, "categoria");
        this.detalle = Objects.requireNonNull(detalle, "detalle");
        // Equivalente al oneOf del esquema: el detalle debe ser el de la misma categoría.
        if (detalle.categoria() != categoria) {
            throw new IllegalArgumentException(
                    "El detalle de " + detalle.categoria() + " no corresponde a la categoría " + categoria);
        }
        this.descripcion = descripcion;
        this.coordenada = Objects.requireNonNull(coordenada, "coordenada");
        this.contacto = contacto;
        this.estadoPublicacion = Objects.requireNonNull(estadoPublicacion, "estadoPublicacion");
        this.prestadorActivo = prestadorActivo;
        this.condicionesAccesibilidad = condicionesAccesibilidad == null ? Set.of() : Set.copyOf(condicionesAccesibilidad);
        this.horarioAtencion = horarioAtencion;
        this.precioBase = detalle instanceof DetalleAlojamiento alojamiento
                ? precioDesde(alojamiento, precioBase)
                : Validaciones.noNegativoOpcional(precioBase, "precioBase");
        this.imagenes = Validaciones.lista(imagenes);
    }

    private static BigDecimal precioDesde(DetalleAlojamiento alojamiento, BigDecimal recibido) {
        BigDecimal desde = alojamiento.precioDesde();
        if (recibido != null && recibido.compareTo(desde) != 0) {
            throw new IllegalArgumentException(
                    "precioBase de un alojamiento debe ser el menor precioNoche de sus unidades (" + desde + ")");
        }
        return desde;
    }

    /** RF16 y RF22: la búsqueda pública solo muestra servicios Publicados de prestadores con la cuenta activa. */
    public boolean esVisibleEnBusquedaPublica() {
        return estadoPublicacion == EstadoPublicacion.Publicado && prestadorActivo;
    }

    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    public UUID getPrestadorId() {
        return prestadorId;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public DetalleCategoria getDetalle() {
        return detalle;
    }

    public Optional<String> getDescripcion() {
        return Optional.ofNullable(descripcion);
    }

    public Coordenada getCoordenada() {
        return coordenada;
    }

    public Optional<Contacto> getContacto() {
        return Optional.ofNullable(contacto);
    }

    public EstadoPublicacion getEstadoPublicacion() {
        return estadoPublicacion;
    }

    public boolean isPrestadorActivo() {
        return prestadorActivo;
    }

    public Set<CondicionAccesibilidad> getCondicionesAccesibilidad() {
        return condicionesAccesibilidad;
    }

    public Optional<String> getHorarioAtencion() {
        return Optional.ofNullable(horarioAtencion);
    }

    /**
     * Precio «desde». Opcional en el esquema; RF37 lo exige en la práctica (ver «Pendientes para el SAD»). En
     * Alojamiento siempre existe: es el menor precio por noche de sus unidades.
     */
    public Optional<BigDecimal> getPrecioBase() {
        return Optional.ofNullable(precioBase);
    }

    public List<String> getImagenes() {
        return imagenes;
    }
}
