import { solicitar } from '../../shared/api/httpClient'
import type { FiltrosReservas, Reserva } from '../reservas/reservasApi'

/**
 * Historial de reservas del Prestador (RF12; docs/sad/api/ms-booking.md, «Reservas del Prestador»; DEC-191, DEC-192):
 * las reservas de sus servicios, 20 por página, sin datos del Turista y sin `cancelableHasta` (el Prestador no cancela).
 */

export type ReservaPrestador = Omit<Reserva, 'cancelableHasta'>

export interface PaginaReservasPrestador {
  elementos: ReservaPrestador[]
  /** Desde 0, como la API. */
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

export interface FiltrosReservasPrestador extends FiltrosReservas {
  /** Id de un servicio propio (ObjectId en texto). */
  servicio?: string
}

/** La página de la pantalla empieza en 1; la de la API, en 0. */
export function consultarReservasPrestador(filtros: FiltrosReservasPrestador, pagina: number): Promise<PaginaReservasPrestador> {
  const consulta = new URLSearchParams()
  if (filtros.desde) consulta.set('desde', filtros.desde)
  if (filtros.hasta) consulta.set('hasta', filtros.hasta)
  if (filtros.estados.length > 0) consulta.set('estado', filtros.estados.join(','))
  if (filtros.servicio) consulta.set('servicio', filtros.servicio)
  consulta.set('pagina', String(Math.max(0, pagina - 1)))
  return solicitar<PaginaReservasPrestador>(`/api/v1/reservas/prestador?${consulta}`)
}
