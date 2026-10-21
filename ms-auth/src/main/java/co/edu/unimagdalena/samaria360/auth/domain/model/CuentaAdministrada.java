package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.EstadoCambiadoException;

import java.util.Objects;
import java.util.UUID;

/**
 * Cuenta de un turista o de un prestador vista por el Administrador (RF28, RF22, RF62). Transiciones válidas (D4,
 * DEC-43; cualquier otra lanza {@link EstadoCambiadoException}, que es el 409 de DEC-39):
 * <ul>
 *   <li>prestador: Pendiente → Activo | Rechazado; Activo → Inactivo (Rechazado e Inactivo no se reactivan, DEC-37);</li>
 *   <li>turista: Activo ↔ Desactivado.</li>
 * </ul>
 * Cada transición se aplica con la versión que se leyó y la sube en uno ({@link CambioEstadoCuenta#versionNueva()}).
 */
public final class CuentaAdministrada {

    private final UUID id;
    private final Rol rol;
    private final EstadoCuenta cuenta;
    private final EstadoValidacion validacion;
    private final long version;

    /** @param validacion estado de validación del prestador; nulo si el rol es Turista */
    public CuentaAdministrada(UUID id, Rol rol, EstadoCuenta cuenta, EstadoValidacion validacion, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.rol = Objects.requireNonNull(rol, "rol");
        this.cuenta = Objects.requireNonNull(cuenta, "cuenta");
        if (rol != Rol.Prestador && rol != Rol.Turista) {
            throw new IllegalArgumentException("Solo se administran turistas y prestadores");
        }
        if (rol == Rol.Prestador && validacion == null) {
            throw new IllegalArgumentException("Un prestador tiene estado de validación");
        }
        if (rol == Rol.Turista && validacion != null) {
            throw new IllegalArgumentException("Solo un prestador tiene estado de validación");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version no puede ser negativa");
        }
        this.validacion = validacion;
        this.version = version;
    }

    public EstadoAdministrativo estado() {
        return rol == Rol.Prestador
                ? EstadoAdministrativo.dePrestador(validacion, cuenta)
                : EstadoAdministrativo.deTurista(cuenta);
    }

    public CambioEstadoCuenta aprobar() {
        exigir(Rol.Prestador, EstadoAdministrativo.Pendiente);
        return cambio(cuenta, EstadoValidacion.Activo, null);
    }

    public CambioEstadoCuenta rechazar(JustificacionRechazo justificacion) {
        Objects.requireNonNull(justificacion, "justificacion");
        exigir(Rol.Prestador, EstadoAdministrativo.Pendiente);
        return cambio(cuenta, EstadoValidacion.Rechazado, justificacion.texto());
    }

    public CambioEstadoCuenta inactivar() {
        exigir(Rol.Prestador, EstadoAdministrativo.Activo);
        return cambio(EstadoCuenta.Inactivo, validacion, null);
    }

    public CambioEstadoCuenta desactivar() {
        exigir(Rol.Turista, EstadoAdministrativo.Activo);
        return cambio(EstadoCuenta.Desactivado, null, null);
    }

    public CambioEstadoCuenta reactivar() {
        exigir(Rol.Turista, EstadoAdministrativo.Desactivado);
        return cambio(EstadoCuenta.Activo, null, null);
    }

    private void exigir(Rol esperado, EstadoAdministrativo origen) {
        if (rol != esperado) {
            throw new IllegalStateException("La acción no corresponde al rol " + rol);
        }
        if (estado() != origen) {
            throw new EstadoCambiadoException(estado());
        }
    }

    private CambioEstadoCuenta cambio(EstadoCuenta cuentaNueva, EstadoValidacion validacionNueva, String motivo) {
        EstadoAdministrativo estadoNuevo = rol == Rol.Prestador
                ? EstadoAdministrativo.dePrestador(validacionNueva, cuentaNueva)
                : EstadoAdministrativo.deTurista(cuentaNueva);
        return new CambioEstadoCuenta(id, rol, version, cuenta, validacion, cuentaNueva, validacionNueva, motivo,
                estadoNuevo);
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

    public EstadoValidacion getValidacion() {
        return validacion;
    }

    public long getVersion() {
        return version;
    }
}
