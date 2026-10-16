import { localeDe } from '../catalogo/formato'

/**
 * Fechas de las reservas: siempre en la hora de Santa Marta (America/Bogota), como ms-booking. Una fecha del calendario
 * («aaaa-mm-dd») se calcula en UTC para que sumar días no se corra por la zona horaria del navegador.
 */
export const ZONA_SANTA_MARTA = 'America/Bogota'

/** ms-booking publica disponibilidad para 60 días: las noches (o los días de tour) van de hoy a hoy + 59. */
export const DIAS_VENTANA = 60
/** Máximo de noches de una estadía (D2). */
export const MAXIMO_NOCHES = 14

const FECHA_ISO = /^(\d{4})-(\d{2})-(\d{2})$/
const MS_DIA = 86_400_000

const formatoIso = new Intl.DateTimeFormat('en-CA', {
  timeZone: ZONA_SANTA_MARTA,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

/** Hoy en Santa Marta, sin importar la zona horaria del navegador. */
export function hoyBogota(ahora: Date = new Date()): string {
  const partes = Object.fromEntries(formatoIso.formatToParts(ahora).map((parte) => [parte.type, parte.value]))
  return `${partes.year}-${partes.month}-${partes.day}`
}

function aUtc(fecha: string): number {
  const [, anio, mes, dia] = FECHA_ISO.exec(fecha)!
  return Date.UTC(Number(anio), Number(mes) - 1, Number(dia))
}

/** «aaaa-mm-dd» de una fecha que existe en el calendario (no acepta 2026-02-30). */
export function esFechaIso(texto: unknown): texto is string {
  if (typeof texto !== 'string' || !FECHA_ISO.test(texto)) return false
  return new Date(aUtc(texto)).toISOString().slice(0, 10) === texto
}

export function sumarDias(fecha: string, dias: number): string {
  return new Date(aUtc(fecha) + dias * MS_DIA).toISOString().slice(0, 10)
}

/** Días de `desde` a `hasta` (negativo si `hasta` es anterior). */
export function diasEntre(desde: string, hasta: string): number {
  return Math.round((aUtc(hasta) - aUtc(desde)) / MS_DIA)
}

/** Las fechas de un rango semiabierto [desde, hasta): las noches de una estadía. */
export function fechasDelRango(desde: string, hasta: string): string[] {
  return Array.from({ length: Math.max(0, diasEntre(desde, hasta)) }, (_, i) => sumarDias(desde, i))
}

/** Ventana de reserva a partir de hoy: última noche (o día de tour) y última fecha de salida posible. */
export function ventana(hoy: string) {
  return { ultimaNoche: sumarDias(hoy, DIAS_VENTANA - 1), ultimaSalida: sumarDias(hoy, DIAS_VENTANA) }
}

/** Una fecha del calendario con el idioma de la interfaz: «13 dic 2026» o «Dec 13, 2026». */
export function formatearFecha(
  fecha: string,
  idioma: string,
  opciones: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'short', year: 'numeric' },
): string {
  return new Intl.DateTimeFormat(localeDe(idioma), { ...opciones, timeZone: 'UTC' }).format(new Date(aUtc(fecha)))
}

/**
 * En español, 24 horas («18:00», como el check-in «15:00» y la hora de desbloqueo del login); es-CO usaría «06:00 p. m.»,
 * que además deja un punto doble al final de una frase. En inglés, 12 horas («6:00 PM»).
 */
function cicloHorario(idioma: string): Intl.DateTimeFormatOptions['hourCycle'] {
  return idioma.startsWith('es') ? 'h23' : undefined
}

/** Hora de Santa Marta de un instante (ISO 8601 o Date): «15:00» o «3:00 PM». */
export function formatearHora(instante: string | Date, idioma: string): string {
  return new Intl.DateTimeFormat(localeDe(idioma), {
    timeZone: ZONA_SANTA_MARTA,
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: cicloHorario(idioma),
  }).format(new Date(instante))
}

/** Fecha y hora de Santa Marta de un instante: «12 dic 2026, 08:00». */
export function formatearInstante(instante: string | Date, idioma: string): string {
  return new Intl.DateTimeFormat(localeDe(idioma), {
    timeZone: ZONA_SANTA_MARTA,
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: cicloHorario(idioma),
  }).format(new Date(instante))
}

/** «08:00:00» (respuesta de ms-booking) → «08:00» (lo que se envía y se muestra). */
export function horaCorta(hora: string): string {
  return hora.slice(0, 5)
}
