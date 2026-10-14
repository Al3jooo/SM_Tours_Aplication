package co.edu.unimagdalena.samaria360.auth.domain.model;

/**
 * Motivo por el que un campo del registro no es válido. Es el valor del mapa {@code campos} de DATOS_INVALIDOS: un código
 * estable que el frontend traduce (RF03), nunca un texto.
 */
public enum MotivoInvalido {
    /** Falta el valor o está vacío. */
    REQUERIDO,
    /** El valor no tiene la forma esperada (p. ej. un correo sin «@» o un país que no tiene 2 letras). */
    FORMATO,
    LONGITUD_MINIMA,
    LONGITUD_MAXIMA,
    /** El valor tiene la forma correcta, pero no está en el vocabulario cerrado (país o categoría). */
    NO_PERMITIDO,
    /** El mismo valor aparece más de una vez (categorías de interés). */
    DUPLICADO
}
