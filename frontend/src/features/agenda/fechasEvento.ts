import { localeDe } from '../catalogo/formato'
import { formatearFecha } from '../reservas/fechasBogota'

/**
 * Fechas de día de los eventos (`yyyy-MM-dd`, DEC-236). Se formatean en UTC, como `formatearFecha`, para que
 * «2026-11-20» sea el 20 en cualquier zona del navegador; `formatearDia` (para instantes) lo correría al 19 (DEC-250).
 */
function aFecha(fecha: string): Date {
  return new Date(`${fecha}T00:00:00Z`)
}

/** «20 nov 2026» en un evento de un día; si no, el rango del idioma: «20–22 nov 2026» o «Nov 20 – 22, 2026». */
export function formatearRango(inicio: string, fin: string, idioma: string): string {
  if (inicio === fin) return formatearFecha(inicio, idioma)
  return new Intl.DateTimeFormat(localeDe(idioma), {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    timeZone: 'UTC',
  }).formatRange(aFecha(inicio), aFecha(fin))
}

/** Día y mes del bloque de la tarjeta de la agenda: «20» y «nov». */
export function partesDia(fecha: string, idioma: string): { dia: string; mes: string } {
  return {
    dia: formatearFecha(fecha, idioma, { day: 'numeric' }),
    mes: formatearFecha(fecha, idioma, { month: 'short' }).replace('.', ''),
  }
}
