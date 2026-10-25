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
    NO_PERMITIDO,
    /** El valor ya está en la lista (p. ej. una condición de accesibilidad o un día repetido, DEC-151). */
    DUPLICADO,
    /** Número menor que el mínimo (p. ej. un precio o un aforo que no es positivo). */
    VALOR_MINIMO,
    /** Número mayor que el máximo técnico (DEC-138). */
    VALOR_MAXIMO,
    /** Lista vacía donde se exige al menos un elemento (p. ej. las unidades de un alojamiento). */
    CANTIDAD_MINIMA,
    /** Lista con más elementos que el tope (p. ej. más de 20 unidades o de 12 salidas). */
    CANTIDAD_MAXIMA
}
