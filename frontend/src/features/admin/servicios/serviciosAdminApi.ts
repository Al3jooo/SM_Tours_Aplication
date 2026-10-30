import { solicitar } from '../../../shared/api/httpClient'
import type { Categoria } from '../../catalogo/catalogoApi'
import type { EstadoPublicacion, ServicioGestion } from '../../servicios/servicioGestion'

/**
 * Contrato de `/api/v1/catalogo/admin/servicios` (ms-catalog, iteración C, A1; docs/sad/api/ms-catalog.md, «Aprobar o
 * rechazar servicio»): solo el rol Administrador, con token (DEC-47). RF21, DEC-128, DEC-144 y DEC-145.
 */
const RUTA = '/api/v1/catalogo/admin/servicios'

/** Un servicio de la cola de revisión (WF-19). */
export interface ServicioCola {
  id: string
  nombreComercial: string
  categoria: Categoria
  idPrestador: string
  rnt: string | null
  estadoPublicacion: EstadoPublicacion
  /** Copia del estado del prestador: con false, el servicio queda oculto aunque se apruebe (RF22). */
  prestadorActivo: boolean
  /** ISO 8601 en UTC. */
  fechaRegistro: string
}

export interface PaginaCola {
  elementos: ServicioCola[]
  /** Desde 0, como la API. */
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

/** Forma de gestión de la vista del Administrador: sin el nombre del prestador ni del revisor (viven en auth_db). */
export interface ServicioAdmin extends ServicioGestion {
  /** `sub` de quien decidió; ausente mientras está Pendiente. */
  revisadoPor?: string | null
  /** Estado de la cuenta según la proyección `cuentas`; ausente si no hay fila (DEC-144). */
  estadoCuentaPrestador?: string | null
}

export type DecisionServicio = 'Aprobar' | 'Rechazar'

/** «Consultar listado pendiente»: del más antiguo al más reciente; `pagina` desde 1 en la pantalla, desde 0 en la API. */
export function listarServiciosAdmin(estado: EstadoPublicacion, pagina: number): Promise<PaginaCola> {
  const parametros = new URLSearchParams({ estado, pagina: String(pagina - 1) })
  return solicitar<PaginaCola>(`${RUTA}?${parametros}`)
}

/** Un servicio en cualquier estado; uno inexistente responde 404 SERVICIO_NO_ENCONTRADO. */
export function consultarServicioAdmin(id: string): Promise<ServicioAdmin> {
  return solicitar<ServicioAdmin>(`${RUTA}/${encodeURIComponent(id)}`)
}

/** Aprobar o rechazar (RF21): la justificación solo viaja al rechazar (al aprobar se ignora, DEC-145). */
export function decidirServicio(id: string, decision: DecisionServicio, justificacion?: string): Promise<ServicioAdmin> {
  return solicitar<ServicioAdmin>(`${RUTA}/${encodeURIComponent(id)}/aprobacion`, {
    method: 'POST',
    cuerpo: decision === 'Rechazar' ? { decision, justificacion: (justificacion ?? '').trim() } : { decision },
  })
}
