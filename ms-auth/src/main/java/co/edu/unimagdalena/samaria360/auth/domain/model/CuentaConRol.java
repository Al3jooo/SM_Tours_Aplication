package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.CambioRolPropioException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaNoElegibleException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.RolCambiadoException;

import java.util.Objects;
import java.util.UUID;

/**
 * Cuenta vista por el Administrador para cambiarle el rol (RF45). Reglas (DEC-82, DEC-105), en este orden:
 * <ol>
 *   <li>el Administrador no cambia su propia cuenta;</li>
 *   <li>el Prestador no es elegible;</li>
 *   <li>el rol actual debe ser el que vio el Administrador (control de concurrencia, como {@code estadoActual});</li>
 *   <li>la cuenta debe estar Activa: un turista desactivado (RF62) no recibe un rol;</li>
 *   <li>revocar (volver a Turista) exige fila en {@code turista}: las cuentas de la semilla no se revocan, pero sí se
 *       modifican.</li>
 * </ol>
 * La regla del último Administrador activo necesita las demás cuentas: está en {@code PoliticaAdministradores}.
 */
public final class CuentaConRol {

    private final UUID id;
    private final Rol rol;
    private final EstadoCuenta cuenta;
    private final boolean tieneRolBase;
    private final long version;

    /** @param tieneRolBase la cuenta tiene fila en {@code turista}, el rol al que vuelve al revocar */
    public CuentaConRol(UUID id, Rol rol, EstadoCuenta cuenta, boolean tieneRolBase, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.rol = Objects.requireNonNull(rol, "rol");
        this.cuenta = Objects.requireNonNull(cuenta, "cuenta");
        if (version < 0) {
            throw new IllegalArgumentException("version no puede ser negativa");
        }
        this.tieneRolBase = tieneRolBase;
        this.version = version;
    }

    /**
     * @param rolNuevo        Turista, Gestor o Administrador, distinto de {@code rolEsperado} (lo valida el caso de uso)
     * @param rolEsperado     el rol que vio el Administrador al pedir el cambio
     * @param idAdministrador quien pide el cambio (claim {@code sub})
     */
    public CambioRol cambiarA(Rol rolNuevo, Rol rolEsperado, UUID idAdministrador) {
        Objects.requireNonNull(rolNuevo, "rolNuevo");
        Objects.requireNonNull(rolEsperado, "rolEsperado");
        Objects.requireNonNull(idAdministrador, "idAdministrador");
        if (id.equals(idAdministrador)) {
            throw new CambioRolPropioException();
        }
        if (rol == Rol.Prestador) {
            throw new CuentaNoElegibleException(MotivoNoElegible.PRESTADOR);
        }
        if (rol != rolEsperado) {
            throw new RolCambiadoException(rol);
        }
        if (cuenta != EstadoCuenta.Activo) {
            throw new CuentaNoElegibleException(MotivoNoElegible.DESACTIVADA);
        }
        if (rolNuevo == Rol.Turista && !tieneRolBase) {
            throw new CuentaNoElegibleException(MotivoNoElegible.SIN_ROL_BASE);
        }
        return new CambioRol(id, rol, rolNuevo, version);
    }

    public UUID getId() {
        return id;
    }

    public Rol getRol() {
        return rol;
    }

    public EstadoCuenta getCuenta() {
        return cuenta;
    }

    public boolean tieneRolBase() {
        return tieneRolBase;
    }

    public long getVersion() {
        return version;
    }
}
