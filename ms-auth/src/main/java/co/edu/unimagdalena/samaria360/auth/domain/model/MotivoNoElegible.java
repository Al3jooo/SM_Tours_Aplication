package co.edu.unimagdalena.samaria360.auth.domain.model;

/**
 * Por qué una cuenta no admite el cambio de rol pedido (RF45, DEC-105). Viaja en la propiedad {@code motivo} del 409
 * CUENTA_NO_ELEGIBLE: es un código estable que el frontend traduce.
 */
public enum MotivoNoElegible {
    /** El Prestador queda fuera de RF45: su validación, sus servicios y sus reservas dependen de ese rol (DEC-82). */
    PRESTADOR,
    /** Revocar sin fila en {@code turista}: no hay un rol base al que volver (las cuentas de la semilla). */
    SIN_ROL_BASE,
    /** La cuenta no está Activa (turista desactivado, RF62): con otro rol no podría reactivarse. */
    DESACTIVADA
}
