package co.edu.unimagdalena.samaria360.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Hecho que ms-auth entrega a ms-catalog y ms-booking (ADR-008): la cuenta {@code idUsuario}, de rol {@code rol}, quedó
 * en el estado {@code estadoCuenta} (el derivado) con la versión {@code version}.
 */
public record EventoCuentaActualizada(UUID idUsuario, Rol rol, EstadoAdministrativo estadoCuenta, long version,
                                      Instant ocurridoEn) {

    public EventoCuentaActualizada {
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(rol, "rol");
        Objects.requireNonNull(estadoCuenta, "estadoCuenta");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        if (version < 1) {
            throw new IllegalArgumentException("version debe ser al menos 1");
        }
    }

    public static EventoCuentaActualizada de(CambioEstadoCuenta cambio, Instant ahora) {
        return new EventoCuentaActualizada(cambio.idUsuario(), cambio.rol(), cambio.estadoNuevo(),
                cambio.versionNueva(), ahora);
    }
}
