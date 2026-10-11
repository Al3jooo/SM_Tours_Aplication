package co.edu.unimagdalena.samaria360.auth.domain.model;

/**
 * Estado de la cuenta de un usuario. Los nombres coinciden con las etiquetas del enum
 * {@code estado_cuenta} de PostgreSQL.
 */
public enum EstadoCuenta {
    Activo,
    Inactivo,
    Desactivado;

    /** Solo una cuenta Activa puede iniciar sesión (RF22, RF62). */
    public boolean permiteIniciarSesion() {
        return this == Activo;
    }
}
