package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Tabla {@code turista_categoria_interes} (RF01, RF53). La base restringe {@code categoria} con un CHECK (V4). */
@Entity
@Table(name = "turista_categoria_interes")
public class TuristaCategoriaInteresEntity {

    @EmbeddedId
    private Clave clave;

    protected TuristaCategoriaInteresEntity() {
    }

    public TuristaCategoriaInteresEntity(UUID idUsuario, String categoria) {
        this.clave = new Clave(idUsuario, categoria);
    }

    public Clave getClave() {
        return clave;
    }

    /** Llave primaria compuesta (id_usuario, categoria). */
    @Embeddable
    public static class Clave implements Serializable {

        @Column(name = "id_usuario")
        private UUID idUsuario;

        @Column(name = "categoria", length = 50)
        private String categoria;

        protected Clave() {
        }

        public Clave(UUID idUsuario, String categoria) {
            this.idUsuario = idUsuario;
            this.categoria = categoria;
        }

        public UUID getIdUsuario() {
            return idUsuario;
        }

        public String getCategoria() {
            return categoria;
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Clave c && Objects.equals(idUsuario, c.idUsuario)
                    && Objects.equals(categoria, c.categoria);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idUsuario, categoria);
        }
    }
}
