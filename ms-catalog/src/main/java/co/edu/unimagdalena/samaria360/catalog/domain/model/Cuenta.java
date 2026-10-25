package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Proyección de una cuenta de ms-auth en {@code catalog_db} (colección {@code cuentas}, ADR-008). {@code version} es la
 * de {@code usuario.version_cuenta} y solo crece: una proyección con una versión menor o igual a la guardada se ignora.
 * {@code rnt} es el del prestador (DEC-126, DEC-146): solo una cuenta de rol Prestador lo tiene, y puede faltar si su
 * último evento es anterior a V17 de ms-auth.
 */
public record Cuenta(UUID id, RolCuenta rol, EstadoCuenta estado, long version, String rnt) {

    public Cuenta {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rol, "rol");
        Objects.requireNonNull(estado, "estado");
        if (version < 1) {
            throw new IllegalArgumentException("version debe ser al menos 1");
        }
        if (rnt != null && (rol != RolCuenta.Prestador || rnt.isBlank())) {
            throw new IllegalArgumentException("Solo un prestador tiene RNT, y no puede estar en blanco");
        }
    }

    /** Una cuenta sin RNT: la de cualquier rol que no sea Prestador. */
    public Cuenta(UUID id, RolCuenta rol, EstadoCuenta estado, long version) {
        this(id, rol, estado, version, null);
    }

    /**
     * RF09 y DEC-125: solo registra servicios un prestador con la cuenta Activa (validada y no inactivada) y con su RNT ya
     * proyectado, porque el servicio guarda una copia (DEC-126).
     */
    public boolean puedeRegistrarServicios() {
        return prestadorActivo() && rnt != null;
    }

    /** RF22: los servicios de un prestador se ven solo mientras su cuenta está Activa (validada y no inactivada). */
    public boolean prestadorActivo() {
        return rol == RolCuenta.Prestador && estado == EstadoCuenta.Activo;
    }
}
