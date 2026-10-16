import { diasEntre, esFechaIso } from './fechasBogota'
import { ESTADOS_RESERVA, type EstadoReserva } from './reservasApi'
import type { CodigoValidacion } from './validacionReserva'

/** Rango máximo de «Mis reservas», contando los dos extremos (como ms-booking). */
export const MAXIMO_DIAS_FILTRO = 366

/** Filtros de WF-11 en la URL: desde, hasta (fechas de la actividad), estado (separados por coma) y página (desde 1). */
export interface CriteriosReservas {
  desde: string
  hasta: string
  estados: EstadoReserva[]
  pagina: number
}

export function leerCriteriosReservas(parametros: URLSearchParams): CriteriosReservas {
  const desde = parametros.get('desde')
  const hasta = parametros.get('hasta')
  const estados = (parametros.get('estado') ?? '')
    .split(',')
    .filter((estado): estado is EstadoReserva => (ESTADOS_RESERVA as readonly string[]).includes(estado))
  const pagina = Number(parametros.get('pagina'))
  return {
    desde: esFechaIso(desde) ? desde : '',
    hasta: esFechaIso(hasta) ? hasta : '',
    // En el orden de las casillas y sin repetidos.
    estados: ESTADOS_RESERVA.filter((estado) => estados.includes(estado)),
    pagina: Number.isInteger(pagina) && pagina >= 1 ? pagina : 1,
  }
}

export function escribirCriteriosReservas(criterios: CriteriosReservas): URLSearchParams {
  const parametros = new URLSearchParams()
  if (criterios.desde) parametros.set('desde', criterios.desde)
  if (criterios.hasta) parametros.set('hasta', criterios.hasta)
  // Siempre en el orden de las casillas: la misma selección da la misma URL (y la misma consulta).
  const estados = ESTADOS_RESERVA.filter((estado) => criterios.estados.includes(estado))
  if (estados.length > 0) parametros.set('estado', estados.join(','))
  if (criterios.pagina > 1) parametros.set('pagina', String(criterios.pagina))
  return parametros
}

/** Las mismas reglas que ms-booking: «hasta» no antes de «desde» y como máximo 366 días. */
export function validarCriteriosReservas(criterios: CriteriosReservas): Partial<Record<'hasta', CodigoValidacion | 'MAXIMO_DIAS'>> {
  if (!criterios.desde || !criterios.hasta) return {}
  if (criterios.hasta < criterios.desde) return { hasta: 'RANGO_INVALIDO' }
  if (diasEntre(criterios.desde, criterios.hasta) + 1 > MAXIMO_DIAS_FILTRO) return { hasta: 'MAXIMO_DIAS' }
  return {}
}

export function hayFiltros(criterios: CriteriosReservas): boolean {
  return criterios.desde !== '' || criterios.hasta !== '' || criterios.estados.length > 0
}
