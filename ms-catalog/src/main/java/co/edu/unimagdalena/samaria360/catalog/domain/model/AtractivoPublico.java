package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Atractivo público registrado por un gestor turístico (RF06, RF07, RF24, RF33). No tiene precio ni estado de
 * publicación: se publica al registrarse.
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

    /**
     * @param id nulo antes de persistir
     * @param categoria texto libre, como en el esquema (p. ej. «Naturaleza», «Cultura»)
     * @param limiteCapacidadCarga RF07; el esquema exige un mínimo de 1
     * @param estadoOperativo opcional en el esquema (RF33)
     */
    public AtractivoPublico(String id, UUID gestorId, String nombre, Coordenada coordenada, String descripcion,
                            String categoria, List<String> imagenes, int limiteCapacidadCarga,
                            EstadoOperativo estadoOperativo, String normasAmbientales,
                            Set<CondicionAccesibilidad> condicionesAccesibilidad) {
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
}
