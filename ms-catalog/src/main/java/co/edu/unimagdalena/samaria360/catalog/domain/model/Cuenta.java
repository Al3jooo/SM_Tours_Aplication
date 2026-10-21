package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Proyección de una cuenta de ms-auth en {@code catalog_db} (colección {@code cuentas}, ADR-008). {@code version} es la
 * de {@code usuario.version_cuenta} y solo crece: una proyección con una versión menor o igual a la guardada se ignora.
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

    /** RF22: los servicios de un prestador se ven solo mientras su cuenta está Activa (validada y no inactivada). */
    public boolean prestadorActivo() {
        return rol == RolCuenta.Prestador && estado == EstadoCuenta.Activo;
    }
}
