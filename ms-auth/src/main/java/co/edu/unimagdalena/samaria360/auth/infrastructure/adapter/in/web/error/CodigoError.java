package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.error;

/**
 * Valor del campo {@code codigo} de los ProblemDetail. Es estable: el frontend lo traduce a ES o EN
 * (RF03, RNF08), así que no se debe renombrar.
 */
public enum CodigoError {
    CREDENCIALES_INVALIDAS,
    CUENTA_BLOQUEADA,
    CUENTA_INACTIVA,
    /** Prestador Pendiente de Validación (RF02, decisión D3). */
    CUENTA_PENDIENTE_VALIDACION,
    /** Prestador Rechazado por el Administrador (RF28, decisión D3). */
    CUENTA_RECHAZADA,
    /** Registro de prestador con un RNT que ya existe (dato público, ADR-006). */
    RNT_YA_REGISTRADO,
    /** Registro sin aceptar la Política de Privacidad (turista y prestador). */
    POLITICAS_NO_ACEPTADAS,
    NO_AUTENTICADO,
    ACCESO_DENEGADO,
    DATOS_INVALIDOS,
    SOLICITUD_INVALIDA,
    ERROR_INTERNO
}
