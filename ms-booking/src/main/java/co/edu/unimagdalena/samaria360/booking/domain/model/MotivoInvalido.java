package co.edu.unimagdalena.samaria360.booking.domain.model;

/**
 * Código de cada campo inválido en {@code DATOS_INVALIDOS}, como en ms-auth: el frontend lo traduce (RF03), así que
 * es estable y nunca es un texto.
 */
public enum MotivoInvalido {
    REQUERIDO,
    FORMATO,
    VALOR_MINIMO,
    NO_PERMITIDO,
    FECHA_PASADA,
    RANGO_INVALIDO,
    MAXIMO_NOCHES,
    MAXIMO_DIAS,
    MAXIMO_ELEMENTOS,
    EXCEDE_CAPACIDAD,
    EXCEDE_AFORO,
    CATEGORIA_NO_RESERVABLE
}
