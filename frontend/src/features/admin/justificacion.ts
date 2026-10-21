/** Máximo de la justificación del rechazo (DEC-42; `varchar(500)` y CHECK en V11). */
export const MAXIMO_JUSTIFICACION = 500

/** Caracteres que cuentan: sin los espacios de los extremos, como `strip()` en ms-auth (DEC-42). */
export function longitudJustificacion(texto: string): number {
  return texto.trim().length
}

/** Mismos códigos que `campos.justificacion` del 400 DATOS_INVALIDOS; null si se puede enviar. */
export function validarJustificacion(texto: string): 'REQUERIDO' | 'LONGITUD_MAXIMA' | null {
  const longitud = longitudJustificacion(texto)
  if (longitud === 0) return 'REQUERIDO'
  if (longitud > MAXIMO_JUSTIFICACION) return 'LONGITUD_MAXIMA'
  return null
}
