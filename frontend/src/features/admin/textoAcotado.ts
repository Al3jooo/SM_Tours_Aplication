/** Límites del contenido público (DEC-61 y DEC-58): los mismos que el dominio y el validador M005 de ms-catalog. */
export const MAXIMO_TITULO = 150
export const MAXIMO_CUERPO = 2000
export const MAXIMO_MENSAJE = 300

/**
 * Caracteres que cuentan: sin los espacios de los extremos (como `strip()` en ms-catalog) y en puntos de código, como
 * `maxLength` de MongoDB: un emoji cuenta uno (DEC-61). `justificacion.ts` (WF-12) cuenta unidades UTF-16 porque su
 * límite es el `varchar` de PostgreSQL; aquí no sirve.
 */
export function longitudTexto(texto: string): number {
  return [...texto.trim()].length
}

/** Mismos códigos que `campos.<campo>` del 400 DATOS_INVALIDOS; null si se puede enviar. */
export function validarTexto(texto: string, maximo: number): 'REQUERIDO' | 'LONGITUD_MAXIMA' | null {
  const longitud = longitudTexto(texto)
  if (longitud === 0) return 'REQUERIDO'
  if (longitud > maximo) return 'LONGITUD_MAXIMA'
  return null
}

/** Primeros `maximo` puntos de código con «…» si se corta: nunca parte un emoji ni un par sustituto. */
export function extracto(texto: string, maximo: number): string {
  const caracteres = [...texto.trim()]
  return caracteres.length <= maximo ? caracteres.join('') : `${caracteres.slice(0, maximo).join('').trimEnd()}…`
}
