package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Tabla {@code prestador} (subtipo 1:1 de usuario). {@code correo_corporativo} es el contacto público (RF43); al
 * registrarse (RF02) se llena con el mismo valor que {@code usuario.correo}, la credencial (decisión D2).
 */
@Entity
@Table(name = "prestador")
public class PrestadorEntity {

    @Id
    @Column(name = "id_usuario")
    private UUID idUsuario;

    /** Opcional desde V5. */
    @Column(name = "razon_social", length = 150)
    private String razonSocial;

    @Column(name = "rnt", nullable = false, unique = true, length = 50)
    private String rnt;

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "nombre_representante_legal", nullable = false, length = 150)
    private String nombreRepresentanteLegal;

    @Column(name = "correo_corporativo", nullable = false, length = 100)
    private String correoCorporativo;

    /** Texto de {@code EstadoValidacion} (CHECK en V1). */
    @Column(name = "estado_validacion", nullable = false, length = 50)
    private String estadoValidacion;

    /** V9: sin DEFAULT, cada inserción envía el valor. */
    @Column(name = "acepto_politicas", nullable = false)
    private boolean aceptoPoliticas;

    protected PrestadorEntity() {
    }

    public PrestadorEntity(UUID idUsuario, String razonSocial, String rnt, String telefono,
                           String nombreRepresentanteLegal, String correoCorporativo, String estadoValidacion,
                           boolean aceptoPoliticas) {
        this.idUsuario = idUsuario;
        this.razonSocial = razonSocial;
        this.rnt = rnt;
        this.telefono = telefono;
        this.nombreRepresentanteLegal = nombreRepresentanteLegal;
        this.correoCorporativo = correoCorporativo;
        this.estadoValidacion = estadoValidacion;
        this.aceptoPoliticas = aceptoPoliticas;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public String getEstadoValidacion() {
        return estadoValidacion;
    }
}
