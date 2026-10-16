import { solicitar } from '../../shared/api/httpClient'
import { decimalExacto } from './dinero'

/** Contrato de ms-booking (`docs/sad/api/ms-booking.md`), a través del gateway. */
const BASE = '/api/v1/reservas'

export type CategoriaReservable = 'Tour' | 'Alojamiento'
export const CATEGORIAS_RESERVABLES: readonly string[] = ['Tour', 'Alojamiento']

export const ESTADOS_RESERVA = ['Confirmada', 'Cancelada', 'Completada'] as const
export type EstadoReserva = (typeof ESTADOS_RESERVA)[number]

export interface CupoDia {
  fecha: string
  cuposTotales: number
  cuposDisponibles: number
}

export interface CupoHorario extends CupoDia {
  /** «08:00:00» */
  hora: string
}

export interface Disponibilidad {
  idServicio: string
  desde: string
  hasta: string
  /** null si no hay inventario en el rango. */
  categoria: CategoriaReservable | null
  unidades: { idUnidad: string; noches: CupoDia[] }[]
  horarios: CupoHorario[]
  horaCheckIn: string
  horaCheckOut: string
  plazoCancelacionHoras: number
}

/** Resumen para las tarjetas (RF11). */
export type EstadoResumen = 'CON_CUPOS' | 'AGOTADO' | 'SIN_INVENTARIO'

export interface Reserva {
  idReserva: string
  estado: EstadoReserva
  idServicio: string
  nombreServicio: string
  categoria: CategoriaReservable
  idUnidad: string | null
  nombreUnidad: string | null
  fechaEntrada: string
  fechaSalida: string | null
  horaInicio: string | null
  fechaInicio: string
  fechaFin: string
  cantidad: number
  personas: number
  precioUnitario: number | string
  unidadCobro: 'Noche' | 'Persona'
  precioTotal: number | string
  moneda: string
  fechaCreacion: string
  fechaCancelacion: string | null
  /** Lo calcula ms-booking (inicio − 24 h, RF44); el cliente no recalcula el plazo. */
  cancelableHasta: string
}

export interface PaginaReservas {
  elementos: Reserva[]
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

export interface FiltrosReservas {
  desde?: string
  hasta?: string
  estados: EstadoReserva[]
}

export function consultarDisponibilidad(servicio: string, desde: string, hasta: string): Promise<Disponibilidad> {
  return solicitar<Disponibilidad>(`${BASE}/disponibilidad?${new URLSearchParams({ servicio, desde, hasta })}`)
}

export function consultarResumen(ids: string[], desde: string, hasta: string): Promise<Record<string, EstadoResumen>> {
  const consulta = new URLSearchParams({ servicios: ids.join(','), desde, hasta })
  return solicitar<Record<string, EstadoResumen>>(`${BASE}/disponibilidad/resumen?${consulta}`)
}

/** Pedido de una reserva, con el total en centavos (ver dinero.ts). */
export type PedidoReserva =
  | {
      categoria: 'Alojamiento'
      idServicio: string
      idUnidad: string
      fechaEntrada: string
      fechaSalida: string
      cantidad: number
      personas: number
      totalCentavos: number
    }
  | {
      categoria: 'Tour'
      idServicio: string
      fechaEntrada: string
      /** «08:00» */
      horaInicio: string
      personas: number
      totalCentavos: number
    }

/**
 * Cuerpo JSON canónico de POST /reservas: siempre las mismas claves en el mismo orden, y `precioTotalEsperado` escrito
 * como número decimal exacto («520001.00», sin comillas). El mismo texto identifica el intento (idempotencia) y se
 * envía tal cual.
 */
export function cuerpoReserva(pedido: PedidoReserva): string {
  const campos: [string, string | number][] =
    pedido.categoria === 'Alojamiento'
      ? [
          ['idServicio', pedido.idServicio],
          ['idUnidad', pedido.idUnidad],
          ['fechaEntrada', pedido.fechaEntrada],
          ['fechaSalida', pedido.fechaSalida],
          ['cantidad', pedido.cantidad],
          ['personas', pedido.personas],
        ]
      : [
          ['idServicio', pedido.idServicio],
          ['fechaEntrada', pedido.fechaEntrada],
          ['horaInicio', pedido.horaInicio],
          ['personas', pedido.personas],
        ]
  const texto = campos.map(([clave, valor]) => `${JSON.stringify(clave)}:${JSON.stringify(valor)}`)
  return `{${texto.join(',')},"precioTotalEsperado":${decimalExacto(pedido.totalCentavos)}}`
}

export function reservar(cuerpo: string, claveIdempotencia: string): Promise<Reserva> {
  return solicitar<Reserva>(BASE, {
    method: 'POST',
    cuerpoTexto: cuerpo,
    cabeceras: { 'Idempotency-Key': claveIdempotencia },
  })
}

/** Mis reservas (RF12, turista). La página de la pantalla empieza en 1; la de la API, en 0. */
export function consultarMisReservas(filtros: FiltrosReservas, pagina: number): Promise<PaginaReservas> {
  const consulta = new URLSearchParams()
  if (filtros.desde) consulta.set('desde', filtros.desde)
  if (filtros.hasta) consulta.set('hasta', filtros.hasta)
  if (filtros.estados.length > 0) consulta.set('estado', filtros.estados.join(','))
  consulta.set('pagina', String(Math.max(0, pagina - 1)))
  return solicitar<PaginaReservas>(`${BASE}/mias?${consulta}`)
}

export function cancelarReserva(idReserva: string): Promise<Reserva> {
  return solicitar<Reserva>(`${BASE}/${encodeURIComponent(idReserva)}/cancelacion`, { method: 'POST' })
}
