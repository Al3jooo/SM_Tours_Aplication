import { solicitar } from '../../shared/api/httpClient'

/** Contrato de GET /api/v1/catalogo/oferta (ms-catalog: OfertaController y sus DTO), a través del gateway. */

export const CATEGORIAS = ['Alojamiento', 'Gastronomia', 'Tour', 'AlquilerEquipo'] as const
export type Categoria = (typeof CATEGORIAS)[number]

export const CONDICIONES_ACCESIBILIDAD = ['MovilidadReducida', 'Visual', 'Auditiva'] as const
export type CondicionAccesibilidad = (typeof CONDICIONES_ACCESIBILIDAD)[number]

export const ORDENES = ['Distancia', 'PrecioAscendente', 'PrecioDescendente', 'Nombre'] as const
export type Orden = (typeof ORDENES)[number]

export type EstadoOperativo = 'Abierto' | 'Cerrado_temporalmente' | 'En_mantenimiento'

export interface ResultadoOferta {
  tipo: 'Servicio' | 'Atractivo'
  id: string
  nombre: string
  /** Categoría del servicio (enum) o texto libre del atractivo. */
  categoria: string | null
  descripcionCorta: string | null
  /** Nulo en los atractivos. */
  precio: number | null
  imagen: string | null
  coordenada: { longitud: number; latitud: number }
  /** Solo si la búsqueda tiene ubicación (RF38). */
  distanciaKm: number | null
  accesibilidad: CondicionAccesibilidad[]
  /** Solo en los atractivos (RF33). */
  estadoOperativo: EstadoOperativo | null
}

export interface PaginaOferta {
  elementos: ResultadoOferta[]
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

export interface Ubicacion {
  latitud: number
  longitud: number
}

/** Criterios de la pantalla. La página empieza en 1; la API la recibe desde 0. */
export interface CriteriosBusqueda {
  texto?: string
  categoria?: Categoria
  precioMax?: number
  distanciaKm?: number
  accesibilidad: CondicionAccesibilidad[]
  orden?: Orden
  pagina: number
  tamano?: number
  /** Nunca va en la URL de la pantalla (privacidad); solo a la API. */
  ubicacion?: Ubicacion
}

export const TAMANO_PAGINA = 10

/**
 * Consulta para la API. Sin ubicación, la distancia máxima y el orden por distancia no se envían: la API los
 * rechazaría (RF38, RF61) y la pantalla los muestra deshabilitados.
 */
export function consultaApi(criterios: CriteriosBusqueda): string {
  const parametros = new URLSearchParams()
  if (criterios.texto) parametros.set('texto', criterios.texto)
  if (criterios.categoria) parametros.set('categoria', criterios.categoria)
  if (criterios.precioMax !== undefined) parametros.set('precioMax', String(criterios.precioMax))
  if (criterios.ubicacion) {
    parametros.set('lat', String(criterios.ubicacion.latitud))
    parametros.set('lng', String(criterios.ubicacion.longitud))
    if (criterios.distanciaKm !== undefined) parametros.set('distanciaKm', String(criterios.distanciaKm))
  }
  criterios.accesibilidad.forEach((condicion) => parametros.append('accesibilidad', condicion))
  if (criterios.orden && (criterios.orden !== 'Distancia' || criterios.ubicacion)) {
    parametros.set('orden', criterios.orden)
  }
  parametros.set('pagina', String(Math.max(0, criterios.pagina - 1)))
  parametros.set('tamano', String(criterios.tamano ?? TAMANO_PAGINA))
  return parametros.toString()
}

export function buscarOferta(criterios: CriteriosBusqueda): Promise<PaginaOferta> {
  return solicitar<PaginaOferta>(`/api/v1/catalogo/oferta?${consultaApi(criterios)}`)
}
