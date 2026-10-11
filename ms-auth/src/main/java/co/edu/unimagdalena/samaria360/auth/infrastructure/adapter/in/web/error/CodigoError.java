package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.error;

/**
 * Valor del campo {@code codigo} de los ProblemDetail. Es estable: el frontend lo traduce a ES o EN
 * (RF03, RNF08), así que no se debe renombrar.
 */
public enum CodigoError {
    CREDENCIALES_INVALIDAS,
    CUENTA_BLOQUEADA,
    CUENTA_INACTIVA,
    NO_AUTENTICADO,
    ACCESO_DENEGADO,
    DATOS_INVALIDOS,
    SOLICITUD_INVALIDA,
    ERROR_INTERNO
}
