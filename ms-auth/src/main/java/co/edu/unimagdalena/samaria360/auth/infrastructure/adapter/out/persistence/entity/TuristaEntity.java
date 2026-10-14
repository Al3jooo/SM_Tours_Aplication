package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Tabla {@code turista} (subtipo 1:1 de usuario). Solo se inserta al registrarse (RF01). */
@Entity
@Table(name = "turista")
public class TuristaEntity {

    @Id
    @Column(name = "id_usuario")
    private UUID idUsuario;

    @Column(name = "primer_nombre", nullable = false, length = 50)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 50)
    private String segundoNombre;

    @Column(name = "primer_apellido", nullable = false, length = 50)
    private String primerApellido;

    @Column(name = "segundo_apellido", length = 50)
    private String segundoApellido;

    /** ISO 3166-1 alfa-2 (V8). */
    @Column(name = "pais_origen", nullable = false, length = 2)
    private String paisOrigen;

    @Column(name = "acepto_politicas", nullable = false)
    private boolean aceptoPoliticas;

    protected TuristaEntity() {
    }

    public TuristaEntity(UUID idUsuario, String primerNombre, String segundoNombre, String primerApellido,
                         String segundoApellido, String paisOrigen, boolean aceptoPoliticas) {
        this.idUsuario = idUsuario;
        this.primerNombre = primerNombre;
        this.segundoNombre = segundoNombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
        this.paisOrigen = paisOrigen;
        this.aceptoPoliticas = aceptoPoliticas;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }
}
