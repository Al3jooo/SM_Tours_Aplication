package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Proyección de una cuenta de ms-auth en {@code reservas_db} (tabla {@code cuenta}, ADR-008). {@code version} es la de
 * {@code usuario.version_cuenta} y solo crece: una proyección con una versión menor o igual a la guardada se ignora.
 */
public record Cuenta(UUID id, RolCuenta rol, EstadoCuenta estado, long version) {

    public Cuenta {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rol, "rol");
        Objects.requireNonNull(estado, "estado");
        if (version < 1) {
            throw new IllegalArgumentException("version debe ser al menos 1");
        }
    }
}
