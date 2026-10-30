import {
  escribirCriteriosReservas,
  hayFiltros,
  leerCriteriosReservas,
  type CriteriosReservas,
} from '../reservas/filtrosReservas'

/**
 * Filtros del historial del Prestador en la URL (DEC-169, DEC-204): los de «Mis reservas» (desde, hasta y estado, con
 * sus mismas reglas y el tope de 366 días) más `servicio`. La página empieza en 1.
 */
export interface CriteriosHistorialPrestador extends CriteriosReservas {
  servicio: string
}

const OBJECT_ID = /^[0-9a-f]{24}$/i

export function esIdServicio(texto: string | null): texto is string {
  return texto !== null && OBJECT_ID.test(texto)
}

export function leerCriteriosHistorial(parametros: URLSearchParams): CriteriosHistorialPrestador {
  const servicio = parametros.get('servicio')
  return { ...leerCriteriosReservas(parametros), servicio: esIdServicio(servicio) ? servicio : '' }
}

/** Orden fijo: desde, hasta, estado, servicio, página (la misma selección da la misma URL). */
export function escribirCriteriosHistorial(criterios: CriteriosHistorialPrestador): URLSearchParams {
  const parametros = escribirCriteriosReservas({ ...criterios, pagina: 1 })
  if (criterios.servicio) parametros.set('servicio', criterios.servicio)
  if (criterios.pagina > 1) parametros.set('pagina', String(criterios.pagina))
  return parametros
}

/** Un `servicio` que no es un ObjectId se quita de la URL (con `replace`). */
export function servicioInvalido(parametros: URLSearchParams): boolean {
  return parametros.has('servicio') && !esIdServicio(parametros.get('servicio'))
}

export function hayFiltrosHistorial(criterios: CriteriosHistorialPrestador): boolean {
  return hayFiltros(criterios) || criterios.servicio !== ''
}
