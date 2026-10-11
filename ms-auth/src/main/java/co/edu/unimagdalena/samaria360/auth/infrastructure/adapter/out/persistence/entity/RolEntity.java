package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla {@code rol}. Catálogo fijo cargado por migración: solo lectura. */
@Entity
@Table(name = "rol")
public class RolEntity {

    @Id
    @Column(name = "id_rol")
    private Integer idRol;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    protected RolEntity() {
    }

    public Integer getIdRol() {
        return idRol;
    }

    public String getNombre() {
        return nombre;
    }
}
