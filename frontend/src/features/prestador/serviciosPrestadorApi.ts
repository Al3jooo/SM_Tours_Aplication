import { solicitar } from '../../shared/api/httpClient'
import type { Categoria } from '../catalogo/catalogoApi'
import type { EstadoPublicacion, ServicioGestion } from '../servicios/servicioGestion'

/**
 * Contrato de `/api/v1/catalogo/prestador/servicios` (ms-catalog, iteración C, A1; docs/sad/api/ms-catalog.md): solo
 * el rol Prestador y siempre los servicios del `sub` del token. Peticiones con token (no son públicas, DEC-47).
 * Los estados y la forma de gestión son comunes con la revisión del Administrador (features/servicios, DEC-176).
 */
export { ESTADOS_PUBLICACION } from '../servicios/servicioGestion'
export type { EstadoPublicacion, ServicioGestion } from '../servicios/servicioGestion'

/** Un servicio en el listado de «Mis servicios» (WF-18). */
export interface ServicioResumen {
  id: string
  nombreComercial: string
  categoria: Categoria
  estadoPublicacion: EstadoPublicacion
  /** Solo en los Rechazados (DEC-143). */
  motivoRechazo: string | null
  /** ISO 8601 en UTC. */
  fechaRegistro: string
}

export interface PaginaServicios {
  elementos: ServicioResumen[]
  /** Desde 0, como la API. */
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

const RUTA = '/api/v1/catalogo/prestador/servicios'

/** RF09, RF10, RF37: el servicio nace Pendiente. 201 con Location y la forma de gestión. */
export function registrarServicio(cuerpo: unknown): Promise<ServicioGestion> {
  return solicitar<ServicioGestion>(RUTA, { method: 'POST', cuerpo })
}

/** «Consultar mis servicios» (DEC-129): `pagina` desde 1 en la pantalla, desde 0 en la API. Sin estado, todos. */
export function listarMisServicios(estado: string, pagina: number): Promise<PaginaServicios> {
  const parametros = new URLSearchParams({ pagina: String(pagina - 1) })
  if (estado) parametros.set('estado', estado)
  return solicitar<PaginaServicios>(`${RUTA}?${parametros}`)
}

/** Un servicio propio, en cualquier estado; uno ajeno o inexistente responde 404 SERVICIO_NO_ENCONTRADO. */
export function consultarMiServicio(id: string): Promise<ServicioGestion> {
  return solicitar<ServicioGestion>(`${RUTA}/${encodeURIComponent(id)}`)
}
