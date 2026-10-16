import { esFechaIso } from './fechasBogota'

/**
 * Selección de WF-10 en la URL (Atrás, recarga, enlace compartido y regreso del login). Los valores inválidos se
 * descartan al leer: la pantalla vuelve a pedirlos.
 */
export interface Seleccion {
  /** Alojamiento */
  entrada?: string
  salida?: string
  unidad?: string
  habitaciones?: number
  /** Tour */
  fecha?: string
  /** «08:00» */
  hora?: string
  /** Huéspedes (alojamiento) o personas (tour). */
  personas?: number
}

/** Tope de huéspedes, personas y habitaciones en la URL y en los campos. */
export const MAXIMO_CANTIDAD = 99
const ID = /^[A-Za-z0-9_-]{1,64}$/
const HORA = /^\d{2}:\d{2}$/

function entero(texto: string | null): number | undefined {
  if (texto === null || !/^\d{1,2}$/.test(texto)) return undefined
  const valor = Number(texto)
  return valor >= 1 && valor <= MAXIMO_CANTIDAD ? valor : undefined
}

function fecha(texto: string | null): string | undefined {
  return esFechaIso(texto) ? texto : undefined
}

export function leerSeleccion(parametros: URLSearchParams): Seleccion {
  const unidad = parametros.get('unidad')
  const hora = parametros.get('hora')
  return {
    entrada: fecha(parametros.get('entrada')),
    salida: fecha(parametros.get('salida')),
    unidad: unidad !== null && ID.test(unidad) ? unidad : undefined,
    habitaciones: entero(parametros.get('habitaciones')),
    fecha: fecha(parametros.get('fecha')),
    hora: hora !== null && HORA.test(hora) ? hora : undefined,
    personas: entero(parametros.get('personas')),
  }
}

/** Siempre en el mismo orden; los campos vacíos no se escriben. */
export function escribirSeleccion(seleccion: Seleccion): URLSearchParams {
  const parametros = new URLSearchParams()
  const orden: (keyof Seleccion)[] = ['entrada', 'salida', 'fecha', 'personas', 'unidad', 'habitaciones', 'hora']
  for (const campo of orden) {
    const valor = seleccion[campo]
    if (valor !== undefined && valor !== '') parametros.set(campo, String(valor))
  }
  return parametros
}
