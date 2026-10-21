package co.edu.unimagdalena.samaria360.auth.domain.model;

/**
 * Estado de una cuenta tal como lo administra el Administrador (RF28, RF22, RF62) y como viaja en el evento
 * {@code CuentaActualizada}. Es un estado <b>derivado</b>: el prestador tiene dos columnas ({@code estado_validacion} y
 * {@code estado_cuenta}) y el turista una; aquí se reducen a un solo valor (DEC-43).
 */
public enum EstadoAdministrativo {
    Pendiente,
    Activo,
    Rechazado,
    Inactivo,
    Desactivado;

    /**
     * Estado de un prestador. Función total, con esta precedencia: validación Pendiente → Pendiente; validación
     * Rechazado → Rechazado; validación Activo y cuenta Activa → Activo; validación Activo y cuenta que no es Activa
     * (Inactivo o Desactivado) → Inactivo.
     */
    public static EstadoAdministrativo dePrestador(EstadoValidacion validacion, EstadoCuenta cuenta) {
        return switch (validacion) {
            case Pendiente -> Pendiente;
            case Rechazado -> Rechazado;
            case Activo -> cuenta == EstadoCuenta.Activo ? Activo : Inactivo;
        };
    }

    /**
     * Estado de un turista: Activo o Desactivado (RF62). Un turista con {@code Inactivo} es un dato heredado de antes
     * de esta iteración (Inactivo es el estado de RF22, del prestador): se trata como Desactivado.
     */
    public static EstadoAdministrativo deTurista(EstadoCuenta cuenta) {
        return cuenta == EstadoCuenta.Activo ? Activo : Desactivado;
    }

    public static EstadoAdministrativo desdeTexto(String texto) {
        for (EstadoAdministrativo estado : values()) {
            if (estado.name().equals(texto)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado desconocido: " + texto);
    }
}
