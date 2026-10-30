import { solicitar } from '../../shared/api/httpClient'
import type { Categoria, CondicionAccesibilidad } from '../catalogo/catalogoApi'

/**
 * Contrato de `/api/v1/catalogo/prestador/servicios` (ms-catalog, iteración C, A1; docs/sad/api/ms-catalog.md): solo
 * el rol Prestador y siempre los servicios del `sub` del token. Peticiones con token (no son públicas, DEC-47).
 */
export const ESTADOS_PUBLICACION = ['Pendiente', 'Publicado', 'Rechazado'] as const
export type EstadoPublicacion = (typeof ESTADOS_PUBLICACION)[number]

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

/** Forma de gestión: la respuesta del 201 y del detalle propio (`GET /prestador/servicios/{id}`). */
export interface ServicioGestion {
  id: string
  idPrestador: string
  nombreComercial: string
  categoria: Categoria
  descripcion: string | null
  direccion: string | null
  coordenadas: { longitud: number; latitud: number } | null
  contacto: { telefono: string | null; correo: string | null } | null
  condicionesAccesibilidad: CondicionAccesibilidad[]
  horarioAtencion: string | null
  /** En Alojamiento, el menor `precioNoche` (DEC-127). Número o texto decimal (BigDecimal). */
  precioBase: number | string | null
  moneda: string
  detalleCategoria: Record<string, unknown> | null
  /** Solo Tour (`diasOperacion` y `salidas`) y Alojamiento (`diasOperacion`); null en las demás (DEC-136). */
  oferta: { diasOperacion: string[]; salidas?: string[] | null } | null
  imagenes: string[]
  estadoPublicacion: EstadoPublicacion
  prestadorActivo: boolean
  /** Copia del RNT del prestador (DEC-126). */
  rnt: string | null
  fechaRegistro: string
  motivoRechazo: string | null
  fechaRevision: string | null
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
