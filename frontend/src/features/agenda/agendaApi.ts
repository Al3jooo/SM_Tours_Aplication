import { solicitar } from '../../shared/api/httpClient'
import type { PaginaCuentas } from '../admin/adminApi'

/**
 * Contrato público de la agenda de eventos (`GET /api/v1/catalogo/eventos`, ms-catalog, iteración E, A2;
 * `AgendaController`; docs/sad/api/ms-catalog.md): los eventos vigentes según «hoy» en Bogotá, por `fechaInicio`
 * ascendente, de 10 en 10 (DEC-215). El cliente no recalcula «hoy»: muestra lo que devuelve el servidor.
 */
export interface EventoAgenda {
  id: string
  nombre: string
  /** Día `yyyy-MM-dd` (America/Bogota, DEC-236): se formatea con `formatearRango`, nunca como instante. */
  fechaInicio: string
  fechaFin: string
  ubicacion: string
  descripcion: string
}

export type PaginaEventos<T = EventoAgenda> = PaginaCuentas<T>

/** `pagina` desde 1 en la pantalla, desde 0 en la API. Público y sin token (DEC-47, DEC-239). */
export function listarAgenda(pagina: number): Promise<PaginaEventos> {
  return solicitar<PaginaEventos>(`/api/v1/catalogo/eventos?pagina=${Math.max(0, pagina - 1)}`, { publica: true })
}
