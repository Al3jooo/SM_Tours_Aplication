package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.entity;

import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoCuenta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Tabla {@code usuario}. Solo son modificables los campos que cambia el inicio de sesión. */
@Entity
@Table(name = "usuario")
public class UsuarioEntity {

    @Id
    @Column(name = "id_usuario")
    private UUID idUsuario;

    // EAGER (por defecto en @ManyToOne): el rol se necesita fuera de transacción con open-in-view=false.
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private RolEntity rol;

    @Column(name = "correo", nullable = false, unique = true)
    private String correo;

    @Column(name = "contrasena_hash", nullable = false)
    private String contrasenaHash;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    // columnDefinition: sin él Hibernate espera un tipo llamado como la clase Java y validate falla.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_cuenta", nullable = false, columnDefinition = "estado_cuenta")
    private EstadoCuenta estadoCuenta;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    protected UsuarioEntity() {
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public RolEntity getRol() {
        return rol;
    }

    public String getCorreo() {
        return correo;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public EstadoCuenta getEstadoCuenta() {
        return estadoCuenta;
    }

    public void setEstadoCuenta(EstadoCuenta estadoCuenta) {
        this.estadoCuenta = estadoCuenta;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(Instant bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }
}
