package co.edu.unimagdalena.samaria360.catalog.domain.model;

/**
 * Estado derivado de la cuenta que publica ms-auth (ADR-008): para un prestador reúne su validación (RF28) y el estado de
 * la cuenta (RF22); para un turista es Activo o Desactivado (RF62).
 */
public enum EstadoCuenta {
    Pendiente,
    Activo,
    Rechazado,
    Inactivo,
    Desactivado
}
