package co.edu.unimagdalena.samaria360.catalog.domain.model;

/**
 * Motivo por el que un campo no es válido. Es el valor del mapa {@code campos} de DATOS_INVALIDOS: un código estable que
 * el frontend traduce (RF03), nunca un texto. Mismo vocabulario que ms-auth.
 */
public enum MotivoInvalido {
    /** Falta el valor, está vacío o solo tiene espacios. */
    REQUERIDO,
    /** El valor no tiene la forma esperada (p. ej. una página negativa o que no es un número). */
    FORMATO,
    LONGITUD_MAXIMA,
    /** El valor tiene la forma correcta, pero no está en el vocabulario cerrado (tipo de aviso). */
    NO_PERMITIDO
}
