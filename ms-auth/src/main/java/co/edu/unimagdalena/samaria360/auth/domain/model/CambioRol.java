package co.edu.unimagdalena.samaria360.auth.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Cambio de rol de una cuenta (RF45), con lo que la base debe encontrar para aplicarlo (UPDATE condicional sobre el rol
 * y la versión, como DEC-39) y lo que debe dejar. Asignar es salir de Turista; revocar, volver a Turista; modificar,
 * pasar entre Gestor y Administrador.
 */
public record CambioRol(UUID idUsuario, Rol rolAnterior, Rol rolNuevo, long versionEsperada) {

    public CambioRol {
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(rolAnterior, "rolAnterior");
        Objects.requireNonNull(rolNuevo, "rolNuevo");
        if (rolAnterior == Rol.Prestador || rolNuevo == Rol.Prestador) {
            throw new IllegalArgumentException("El Prestador no cambia de rol (DEC-82)");
        }
        if (rolAnterior == rolNuevo) {
            throw new IllegalArgumentException("El rol nuevo debe ser distinto del actual");
        }
        if (versionEsperada < 0) {
            throw new IllegalArgumentException("versionEsperada no puede ser negativa");
        }
    }

    public long versionNueva() {
        return versionEsperada + 1;
    }

    /** La cuenta deja de ser Administrador: hay que comprobar que quede otro activo (DEC-106). */
    public boolean quitaAdministrador() {
        return rolAnterior == Rol.Administrador;
    }
}
