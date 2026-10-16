import { diasEntre, esFechaIso, MAXIMO_NOCHES, sumarDias, ventana } from './fechasBogota'

/**
 * Validación de WF-10 con los mismos códigos que ms-booking (`DATOS_INVALIDOS`). Valida aunque los campos tengan
 * min y max: una fecha también se puede escribir a mano. Dos códigos son solo del frontend: FUERA_DE_VENTANA (después
 * de la última fecha con inventario publicado) y EXCEDE_CUPO (más de lo que queda).
 */
export type CodigoValidacion =
  | 'REQUERIDO'
  | 'FORMATO'
  | 'FECHA_PASADA'
  | 'FUERA_DE_VENTANA'
  | 'RANGO_INVALIDO'
  | 'MAXIMO_NOCHES'
  | 'VALOR_MINIMO'
  | 'EXCEDE_CAPACIDAD'
  | 'EXCEDE_AFORO'
  | 'EXCEDE_CUPO'

export type Errores<C extends string> = Partial<Record<C, CodigoValidacion>>

/** Entero positivo escrito en un campo numérico; undefined si está vacío; NaN si no es un entero. */
export function leerEntero(texto: string): number | undefined {
  if (texto.trim() === '') return undefined
  return /^\d+$/.test(texto.trim()) ? Number(texto.trim()) : Number.NaN
}

function validarPersonas(personas: number | undefined): CodigoValidacion | undefined {
  if (personas === undefined) return 'REQUERIDO'
  if (Number.isNaN(personas)) return 'FORMATO'
  if (personas < 1) return 'VALOR_MINIMO'
  return undefined
}

function validarDia(dia: string, hoy: string): CodigoValidacion | undefined {
  if (dia === '') return 'REQUERIDO'
  if (!esFechaIso(dia)) return 'FORMATO'
  if (dia < hoy) return 'FECHA_PASADA'
  if (dia > ventana(hoy).ultimaNoche) return 'FUERA_DE_VENTANA'
  return undefined
}

export function validarBusquedaAlojamiento(
  datos: { entrada: string; salida: string; personas: number | undefined },
  hoy: string,
): Errores<'entrada' | 'salida' | 'personas'> {
  const errores: Errores<'entrada' | 'salida' | 'personas'> = {}
  const entrada = validarDia(datos.entrada, hoy)
  if (entrada) errores.entrada = entrada
  if (datos.salida === '') errores.salida = 'REQUERIDO'
  else if (!esFechaIso(datos.salida)) errores.salida = 'FORMATO'
  else if (!entrada) {
    if (datos.salida <= datos.entrada) errores.salida = 'RANGO_INVALIDO'
    else if (diasEntre(datos.entrada, datos.salida) > MAXIMO_NOCHES) errores.salida = 'MAXIMO_NOCHES'
    else if (datos.salida > ventana(hoy).ultimaSalida) errores.salida = 'FUERA_DE_VENTANA'
  }
  const personas = validarPersonas(datos.personas)
  if (personas) errores.personas = personas
  return errores
}

export function validarBusquedaTour(
  datos: { fecha: string; personas: number | undefined },
  hoy: string,
): Errores<'fecha' | 'personas'> {
  const errores: Errores<'fecha' | 'personas'> = {}
  const fecha = validarDia(datos.fecha, hoy)
  if (fecha) errores.fecha = fecha
  const personas = validarPersonas(datos.personas)
  if (personas) errores.personas = personas
  return errores
}

/**
 * Habitaciones de la unidad elegida: de 1 al cupo; los huéspedes deben caber (habitaciones × capacidad). ms-booking
 * marca EXCEDE_CAPACIDAD en `personas`; aquí se muestra en las habitaciones, que es lo que el turista ajusta.
 */
export function validarHabitaciones(
  datos: { habitaciones: number | undefined; personas: number },
  unidad: { capacidadMaxHuespedes: number },
  cupo: number,
): Errores<'habitaciones' | 'personas'> {
  const { habitaciones, personas } = datos
  if (habitaciones === undefined) return { habitaciones: 'REQUERIDO' }
  if (Number.isNaN(habitaciones)) return { habitaciones: 'FORMATO' }
  if (habitaciones < 1) return { habitaciones: 'VALOR_MINIMO' }
  if (habitaciones > cupo) return { habitaciones: 'EXCEDE_CUPO' }
  if (personas > habitaciones * unidad.capacidadMaxHuespedes) return { habitaciones: 'EXCEDE_CAPACIDAD' }
  return {}
}

/** Personas de un tour: como máximo el aforo del grupo y lo que queda en el horario. */
export function validarPersonasTour(personas: number, aforoMaxGrupo: number, cupo: number): Errores<'personas'> {
  if (personas > aforoMaxGrupo) return { personas: 'EXCEDE_AFORO' }
  if (personas > cupo) return { personas: 'EXCEDE_CUPO' }
  return {}
}

/** Límites del campo de salida: de entrada + 1 al menor entre entrada + 14 y la última salida de la ventana. */
export function limitesSalida(entrada: string, hoy: string): { min: string; max: string } | null {
  if (!esFechaIso(entrada)) return null
  const maximo = sumarDias(entrada, MAXIMO_NOCHES)
  const { ultimaSalida } = ventana(hoy)
  return { min: sumarDias(entrada, 1), max: maximo < ultimaSalida ? maximo : ultimaSalida }
}

/**
 * Al cambiar la entrada: si la salida queda fuera de sus límites, se ajusta a entrada + 1 (o se borra si ni eso cabe
 * en la ventana). Devuelve qué pasó, para anunciarlo.
 */
export function ajustarSalida(
  entrada: string,
  salida: string,
  hoy: string,
): { salida: string; ajuste: 'ajustada' | 'borrada' | null } {
  const limites = limitesSalida(entrada, hoy)
  if (!limites || salida === '') return { salida, ajuste: null }
  if (salida >= limites.min && salida <= limites.max) return { salida, ajuste: null }
  return limites.min <= limites.max
    ? { salida: limites.min, ajuste: 'ajustada' }
    : { salida: '', ajuste: 'borrada' }
}
