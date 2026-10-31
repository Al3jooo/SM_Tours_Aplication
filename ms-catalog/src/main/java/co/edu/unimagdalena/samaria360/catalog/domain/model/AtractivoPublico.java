package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Atractivo público registrado por un gestor turístico (RF06, RF07, RF24, RF33, RF58). No tiene precio ni estado de
 * publicación: se publica al registrarse y es visible mientras exista (DEC-87). Es del municipio, no de un Gestor:
 * cualquier Gestor lo edita y {@code gestorId} solo dice quién lo registró (DEC-206).
 */
public final class AtractivoPublico {

    private final String id;
    private final UUID gestorId;
    private final String nombre;
    private final Coordenada coordenada;
    private final String descripcion;
    private final String categoria;
    private final List<String> imagenes;
    private final int limiteCapacidadCarga;
    private final EstadoOperativo estadoOperativo;
    private final String normasAmbientales;
    private final Set<CondicionAccesibilidad> condicionesAccesibilidad;
    private final String horarioAtencion;
    private final Instant fechaRegistro;
    private final Instant fechaActualizacion;
    private final int version;

    /**
     * Sin los campos de la gestión (iteración E): la búsqueda (RF16) solo lee estos.
     *
     * @param id nulo antes de persistir
     * @param categoria texto, como en el esquema (desde M012, «Naturaleza» o «Cultura», DEC-214)
     * @param limiteCapacidadCarga RF07; el esquema exige un mínimo de 1
     * @param estadoOperativo opcional en el esquema (RF33)
     */
    public AtractivoPublico(String id, UUID gestorId, String nombre, Coordenada coordenada, String descripcion,
                            String categoria, List<String> imagenes, int limiteCapacidadCarga,
                            EstadoOperativo estadoOperativo, String normasAmbientales,
                            Set<CondicionAccesibilidad> condicionesAccesibilidad) {
        this(id, gestorId, nombre, coordenada, descripcion, categoria, imagenes, limiteCapacidadCarga, estadoOperativo,
                normasAmbientales, condicionesAccesibilidad, null, null, null, 0);
    }

    /**
     * Admite nulos en los campos opcionales del validador v2 para rehidratar los documentos de la semilla; un registro
     * nuevo solo se crea con {@link RegistroAtractivo}, que exige los de RF06.
     *
     * @param horarioAtencion    RF58 (DEC-208)
     * @param fechaRegistro      nula solo antes de M012
     * @param fechaActualizacion la de la última edición o cambio de estado; nula si nunca se editó
     * @param version            la del documento (DEC-207); en una edición, la que el Gestor leyó
     */
    public AtractivoPublico(String id, UUID gestorId, String nombre, Coordenada coordenada, String descripcion,
                            String categoria, List<String> imagenes, int limiteCapacidadCarga,
                            EstadoOperativo estadoOperativo, String normasAmbientales,
                            Set<CondicionAccesibilidad> condicionesAccesibilidad, String horarioAtencion,
                            Instant fechaRegistro, Instant fechaActualizacion, int version) {
        this.id = id;
        this.gestorId = Objects.requireNonNull(gestorId, "gestorId");
        this.nombre = Validaciones.texto(nombre, "nombre");
        this.coordenada = Objects.requireNonNull(coordenada, "coordenada");
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.imagenes = Validaciones.lista(imagenes);
        this.limiteCapacidadCarga = Validaciones.positivo(limiteCapacidadCarga, "limiteCapacidadCarga");
        this.estadoOperativo = estadoOperativo;
        this.normasAmbientales = normasAmbientales;
        this.condicionesAccesibilidad = condicionesAccesibilidad == null ? Set.of() : Set.copyOf(condicionesAccesibilidad);
        this.horarioAtencion = horarioAtencion;
        this.fechaRegistro = fechaRegistro;
        this.fechaActualizacion = fechaActualizacion;
        this.version = Validaciones.noNegativo(version, "version");
    }

    /** Los datos editables (todos menos el registro, las imágenes y el estado, DEC-206) con la versión leída. */
    AtractivoPublico conDatos(String nombre, Coordenada coordenada, String descripcion, String categoria,
                              int limiteCapacidadCarga, String normasAmbientales,
                              Set<CondicionAccesibilidad> condicionesAccesibilidad, String horarioAtencion,
                              Instant fechaActualizacion, int versionLeida) {
        return new AtractivoPublico(id, gestorId, nombre, coordenada, descripcion, categoria, imagenes,
                limiteCapacidadCarga, estadoOperativo, normasAmbientales, condicionesAccesibilidad, horarioAtencion,
                fechaRegistro, fechaActualizacion, versionLeida);
    }

    /** El estado nuevo (RF33) con la versión leída. */
    AtractivoPublico conEstado(EstadoOperativo estado, Instant fechaActualizacion, int versionLeida) {
        return new AtractivoPublico(id, gestorId, nombre, coordenada, descripcion, categoria, imagenes,
                limiteCapacidadCarga, Objects.requireNonNull(estado), normasAmbientales, condicionesAccesibilidad,
                horarioAtencion, fechaRegistro, fechaActualizacion, versionLeida);
    }

    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    public UUID getGestorId() {
        return gestorId;
    }

    public String getNombre() {
        return nombre;
    }

    public Coordenada getCoordenada() {
        return coordenada;
    }

    public Optional<String> getDescripcion() {
        return Optional.ofNullable(descripcion);
    }

    public Optional<String> getCategoria() {
        return Optional.ofNullable(categoria);
    }

    public List<String> getImagenes() {
        return imagenes;
    }

    public int getLimiteCapacidadCarga() {
        return limiteCapacidadCarga;
    }

    public Optional<EstadoOperativo> getEstadoOperativo() {
        return Optional.ofNullable(estadoOperativo);
    }

    public Optional<String> getNormasAmbientales() {
        return Optional.ofNullable(normasAmbientales);
    }

    public Set<CondicionAccesibilidad> getCondicionesAccesibilidad() {
        return condicionesAccesibilidad;
    }

    public Optional<String> getHorarioAtencion() {
        return Optional.ofNullable(horarioAtencion);
    }

    public Optional<Instant> getFechaRegistro() {
        return Optional.ofNullable(fechaRegistro);
    }

    public Optional<Instant> getFechaActualizacion() {
        return Optional.ofNullable(fechaActualizacion);
    }

    public int getVersion() {
        return version;
    }
}
