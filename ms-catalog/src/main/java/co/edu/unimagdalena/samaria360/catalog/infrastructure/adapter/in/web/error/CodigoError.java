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
    /** No existe una pregunta frecuente con ese id, o el id no es un ObjectId (RF26). */
    FAQ_NO_ENCONTRADA,
    /** Ya hay 200 preguntas frecuentes, el tope de la colección (DEC-62). */
    LIMITE_FAQ,
    /** No existe un aviso con ese id, o el id no es un ObjectId (RF40). */
    AVISO_NO_ENCONTRADO,
    /** El aviso ya no está en el estado de origen: ya estaba despublicado (DEC-64, como DEC-39 en ms-auth). */
    ESTADO_CAMBIADO,
    /** El turista ya tiene 100 favoritos, el tope por turista (DEC-86). */
    LIMITE_FAVORITOS,
    ACCESO_DENEGADO,
    SOLICITUD_INVALIDA,
    ERROR_INTERNO
}
