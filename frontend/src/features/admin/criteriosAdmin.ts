import { ESTADOS_PRESTADOR, ESTADOS_TURISTA } from './adminApi'

export const PESTANAS = ['pendientes', 'prestadores', 'turistas'] as const
export type Pestana = (typeof PESTANAS)[number]

/** Máximo de `q` en ms-auth (más: 400 `q: LONGITUD_MAXIMA`). */
export const MAXIMO_BUSQUEDA = 100

/**
 * Criterios de WF-12 en la URL (DEC-51): `pestana`, `estado`, `q` y `pagina` (desde 1, como «Mis reservas»; la API
 * empieza en 0). Los valores por defecto (pestaña «pendientes», sin estado, sin búsqueda, página 1) no se escriben.
 */
export interface CriteriosAdmin {
  pestana: Pestana
  /** '' = todos. En «pendientes» siempre es '' (la pestaña ya fija Pendiente). */
  estado: string
  q: string
  pagina: number
}

/** Estados que admite el filtro de cada pestaña. */
export function estadosDe(pestana: Pestana): readonly string[] {
  if (pestana === 'prestadores') return ESTADOS_PRESTADOR
  if (pestana === 'turistas') return ESTADOS_TURISTA
  return []
}

export function leerCriteriosAdmin(parametros: URLSearchParams): CriteriosAdmin {
  const leida = parametros.get('pestana')
  const pestana = (PESTANAS as readonly string[]).includes(leida ?? '') ? (leida as Pestana) : 'pendientes'
  const estado = parametros.get('estado') ?? ''
  const q = (parametros.get('q') ?? '').trim().slice(0, MAXIMO_BUSQUEDA)
  const pagina = Number(parametros.get('pagina'))
  return {
    pestana,
    estado: estadosDe(pestana).includes(estado) ? estado : '',
    q,
    pagina: Number.isInteger(pagina) && pagina >= 1 ? pagina : 1,
  }
}

export function escribirCriteriosAdmin(criterios: CriteriosAdmin): URLSearchParams {
  const parametros = new URLSearchParams()
  if (criterios.pestana !== 'pendientes') parametros.set('pestana', criterios.pestana)
  if (criterios.estado && estadosDe(criterios.pestana).includes(criterios.estado)) {
    parametros.set('estado', criterios.estado)
  }
  const q = criterios.q.trim()
  if (q) parametros.set('q', q.slice(0, MAXIMO_BUSQUEDA))
  if (criterios.pagina > 1) parametros.set('pagina', String(criterios.pagina))
  return parametros
}

export function hayFiltrosAdmin(criterios: CriteriosAdmin): boolean {
  return criterios.estado !== '' || criterios.q !== ''
}
