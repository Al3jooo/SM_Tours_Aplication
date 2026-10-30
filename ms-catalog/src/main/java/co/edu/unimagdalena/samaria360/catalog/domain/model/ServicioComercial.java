package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio comercial de un prestador (RF09, RF10, RF37). Inmutable. Se registra Pendiente y el Administrador lo pasa a
 * Publicado o a Rechazado (RF21, DEC-124); el registro y su validación están en {@link RegistroServicio}.
 * <p>
 * Los datos de la iteración C ({@code direccion}, {@code oferta}, {@code rnt}, {@code fechaRegistro} y la revisión) son
 * nulables en el constructor solo para <b>rehidratar</b> documentos anteriores (sin M009 no tienen dirección ni RNT): un
 * servicio nuevo se crea siempre con {@link RegistroServicio}, que los exige (DEC-126, DEC-136, DEC-148).
 * <p>
 * El Prestador edita solo el precio (RF32, {@link ActualizacionPrecio}) y los datos comerciales (RF43,
 * {@link ActualizacionDatosComerciales}); la oferta es inmutable después de la aprobación (DEC-183). {@code version} es
 * el control de concurrencia de esas ediciones y de la decisión del Administrador (DEC-186).
 */
public final class ServicioComercial {

    public static final int LONGITUD_MAXIMA_NOMBRE = 150;
    public static final int LONGITUD_MAXIMA_DIRECCION = 200;

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
    private final String direccion;
    private final Oferta oferta;
    private final String rnt;
    private final Instant fechaRegistro;
    private final Revision revision;
    private final int version;

    /**
     * Un servicio en la versión 0, la de un registro nuevo (RF09).
     *
     * @param id nulo antes de persistir
     * @param prestadorActivo copia desnormalizada del estado de la cuenta del prestador en ms-auth (RF22)
     * @param precioBase precio «desde» (RF37). En Alojamiento se deriva de las unidades (el menor precio por noche):
     *                   puede llegar nulo, y si llega debe coincidir, como exige el {@code $expr} del esquema v2
     * @param oferta     solo en Tour (con salidas) y en Alojamiento (sin salidas), DEC-136
     * @param rnt        copia del RNT del prestador tomada al registrar (DEC-126)
     * @param revision   quién decidió y cuándo (RF21): nula mientras está Pendiente; con motivo si y solo si está
     *                   Rechazado. Un servicio anterior a la iteración C (la semilla) no tiene revisión
     */
    public ServicioComercial(String id, UUID prestadorId, String nombreComercial, Categoria categoria,
                             DetalleCategoria detalle, String descripcion, Coordenada coordenada, Contacto contacto,
                             EstadoPublicacion estadoPublicacion, boolean prestadorActivo,
                             Set<CondicionAccesibilidad> condicionesAccesibilidad, String horarioAtencion,
                             BigDecimal precioBase, List<String> imagenes, String direccion, Oferta oferta, String rnt,
                             Instant fechaRegistro, Revision revision) {
        this(id, prestadorId, nombreComercial, categoria, detalle, descripcion, coordenada, contacto, estadoPublicacion,
                prestadorActivo, condicionesAccesibilidad, horarioAtencion, precioBase, imagenes, direccion, oferta, rnt,
                fechaRegistro, revision, 0);
    }

    /** @param version la del documento (DEC-186): la sube cada edición del Prestador y la decisión del Administrador */
    public ServicioComercial(String id, UUID prestadorId, String nombreComercial, Categoria categoria,
                             DetalleCategoria detalle, String descripcion, Coordenada coordenada, Contacto contacto,
                             EstadoPublicacion estadoPublicacion, boolean prestadorActivo,
                             Set<CondicionAccesibilidad> condicionesAccesibilidad, String horarioAtencion,
                             BigDecimal precioBase, List<String> imagenes, String direccion, Oferta oferta, String rnt,
                             Instant fechaRegistro, Revision revision, int version) {
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
        this.direccion = direccion == null ? null
                : TextoAcotado.exigir(direccion, LONGITUD_MAXIMA_DIRECCION, "direccion");
        if (oferta != null && !oferta.correspondeA(categoria)) {
            throw new IllegalArgumentException("La oferta no corresponde a la categoría " + categoria);
        }
        this.oferta = oferta;
        if (rnt != null && rnt.isBlank()) {
            throw new IllegalArgumentException("rnt no puede estar en blanco");
        }
        this.rnt = rnt;
        this.fechaRegistro = fechaRegistro;
        exigirRevisionCoherente(estadoPublicacion, revision);
        this.revision = revision;
        if (version < 0) {
            throw new IllegalArgumentException("version no puede ser negativa");
        }
        this.version = version;
    }

    /**
     * Una revisión no existe mientras el servicio está Pendiente, y su motivo existe si y solo si quedó Rechazado. Sin
     * revisión es un servicio anterior a la iteración C; el validador v3 exige además el motivo a todo Rechazado.
     */
    private static void exigirRevisionCoherente(EstadoPublicacion estado, Revision revision) {
        if (revision == null) {
            return;
        }
        if (estado == EstadoPublicacion.Pendiente) {
            throw new IllegalArgumentException("Un servicio Pendiente no tiene revisión");
        }
        if ((estado == EstadoPublicacion.Rechazado) != (revision.motivoRechazo() != null)) {
            throw new IllegalArgumentException("El motivo de rechazo existe si y solo si el servicio está Rechazado");
        }
    }

    /** El mismo servicio con otra copia del estado del prestador (RF22, DEC-142). */
    public ServicioComercial conPrestadorActivo(boolean activo) {
        return new ServicioComercial(id, prestadorId, nombreComercial, categoria, detalle, descripcion, coordenada,
                contacto, estadoPublicacion, activo, condicionesAccesibilidad, horarioAtencion, precioBase, imagenes,
                direccion, oferta, rnt, fechaRegistro, revision, version);
    }

    /**
     * RF32: el mismo servicio con otros precios. En Alojamiento el «desde» se vuelve a derivar de las unidades
     * (DEC-127); la oferta y lo demás no cambian (DEC-183). La versión la sube la escritura (DEC-186).
     */
    ServicioComercial conPrecios(BigDecimal nuevoPrecioBase, DetalleCategoria nuevoDetalle) {
        return new ServicioComercial(id, prestadorId, nombreComercial, categoria, nuevoDetalle, descripcion,
                coordenada, contacto, estadoPublicacion, prestadorActivo, condicionesAccesibilidad, horarioAtencion,
                nuevoDetalle instanceof DetalleAlojamiento ? null : nuevoPrecioBase, imagenes, direccion, oferta, rnt,
                fechaRegistro, revision, version);
    }

    /** RF43: el mismo servicio con otro nombre comercial y otro contacto de atención al público (DEC-188, DEC-189). */
    ServicioComercial conDatosComerciales(String nuevoNombre, Contacto nuevoContacto) {
        return new ServicioComercial(id, prestadorId, nuevoNombre, categoria, detalle, descripcion, coordenada,
                nuevoContacto, estadoPublicacion, prestadorActivo, condicionesAccesibilidad, horarioAtencion, precioBase,
                imagenes, direccion, oferta, rnt, fechaRegistro, revision, version);
    }

    /** RF32 y RF43: solo un servicio Pendiente o Publicado se edita; Rechazado es final (DEC-177, DEC-193). */
    public boolean esEditablePorElPrestador() {
        return estadoPublicacion != EstadoPublicacion.Rechazado;
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

    public Optional<String> getDireccion() {
        return Optional.ofNullable(direccion);
    }

    public Optional<Oferta> getOferta() {
        return Optional.ofNullable(oferta);
    }

    public Optional<String> getRnt() {
        return Optional.ofNullable(rnt);
    }

    public Optional<Instant> getFechaRegistro() {
        return Optional.ofNullable(fechaRegistro);
    }

    public Optional<Revision> getRevision() {
        return Optional.ofNullable(revision);
    }

    public int getVersion() {
        return version;
    }
}
