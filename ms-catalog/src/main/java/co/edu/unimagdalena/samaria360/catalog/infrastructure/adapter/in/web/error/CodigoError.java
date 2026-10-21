package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.error;

/**
 * Valor del campo {@code codigo} de los ProblemDetail, con el mismo formato que ms-auth. Es estable: el frontend lo
 * traduce a ES o EN (RF03, RNF08), así que no se debe renombrar.
 */
public enum CodigoError {
    CRITERIOS_INVALIDOS,
    OFERTA_NO_DISPONIBLE,
    NO_AUTENTICADO,
    /** Token de antes de un cambio de estado o de rol de la cuenta: su claim ver es menor que la versión conocida (DEC-29). */
    SESION_INVALIDADA,
    DATOS_INVALIDOS,
    ACCESO_DENEGADO,
    SOLICITUD_INVALIDA,
    ERROR_INTERNO
}
