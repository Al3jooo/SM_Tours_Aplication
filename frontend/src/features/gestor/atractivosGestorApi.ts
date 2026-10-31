import { solicitar } from '../../shared/api/httpClient'
import type { CondicionAccesibilidad, EstadoOperativo } from '../catalogo/catalogoApi'

/**
 * Contrato de `/api/v1/catalogo/gestor/atractivos` (ms-catalog, iteración E, A1; `AtractivoGestorController`;
 * docs/sad/api/ms-catalog.md): solo el rol Gestor, que gestiona cualquier atractivo (DEC-206). Peticiones con token.
 */
export const CATEGORIAS_ATRACTIVO = ['Naturaleza', 'Cultura'] as const
export type CategoriaAtractivo = (typeof CATEGORIAS_ATRACTIVO)[number]

export const ESTADOS_OPERATIVOS = ['Abierto', 'Cerrado_temporalmente', 'En_mantenimiento'] as const satisfies readonly EstadoOperativo[]

/** Forma de gestión (`RespuestaAtractivoGestion`): con quién lo registró, las fechas y la versión (DEC-207). */
export interface AtractivoGestion {
  id: string
  nombre: string
  categoria: string | null
  descripcion: string | null
  coordenadas: { longitud: number; latitud: number }
  imagenes: string[]
  limiteCapacidadCarga: number
  estadoOperativo: EstadoOperativo | null
  normasAmbientales: string | null
  horarioAtencion: string | null
  condicionesAccesibilidad: CondicionAccesibilidad[]
  gestorId: string
  fechaRegistro: string | null
  fechaActualizacion: string | null
  version: number
}

/** Una fila del panel «Atractivos» (WF-24, `ElementoAtractivoGestor`). */
export interface ElementoAtractivo {
  id: string
  nombre: string
  categoria: string | null
  estadoOperativo: EstadoOperativo | null
  limiteCapacidadCarga: number
  /** La portada; null si no tiene. */
  imagen: string | null
  fechaRegistro: string | null
  version: number
}

export interface PaginaAtractivos {
  elementos: ElementoAtractivo[]
  /** Desde 0, como la API. */
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

/** Los datos editables (`SolicitudAtractivo`): la parte `datos` del registro y el cuerpo de la edición. */
export interface DatosAtractivo {
  nombre: string
  categoria: string
  descripcion: string
  coordenadas: { longitud: number | null; latitud: number | null }
  limiteCapacidadCarga: number | null
  /** Solo se lee al registrar (DEC-210). */
  estadoOperativo?: string
  normasAmbientales: string
  horarioAtencion: string
  condicionesAccesibilidad: string[]
}

const RUTA = '/api/v1/catalogo/gestor/atractivos'

/**
 * RF06 (DEC-216, DEC-230): multipart con la parte `datos` (JSON, `application/json`, como exige el backend) y de 1 a 5
 * partes `imagenes`. 201 con el atractivo y `version: 0`.
 */
export function registrarAtractivo(datos: DatosAtractivo, imagenes: File[]): Promise<AtractivoGestion> {
  const formulario = new FormData()
  formulario.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }))
  for (const imagen of imagenes) formulario.append('imagenes', imagen, imagen.name)
  return solicitar<AtractivoGestion>(RUTA, { method: 'POST', formulario })
}

/** Panel «Atractivos» (WF-24): `pagina` desde 1 en la pantalla, desde 0 en la API. Sin filtros (DEC-222). */
export function listarAtractivos(pagina: number): Promise<PaginaAtractivos> {
  return solicitar<PaginaAtractivos>(`${RUTA}?pagina=${pagina - 1}`)
}

export function consultarAtractivoGestion(id: string): Promise<AtractivoGestion> {
  return solicitar<AtractivoGestion>(`${RUTA}/${encodeURIComponent(id)}`)
}

/** RF07, RF24, RF27, RF58: todos los datos editables con la versión leída; 409 ATRACTIVO_MODIFICADO (`versionActual`). */
export function actualizarAtractivo(id: string, datos: DatosAtractivo, version: number): Promise<AtractivoGestion> {
  return solicitar<AtractivoGestion>(`${RUTA}/${encodeURIComponent(id)}`, { method: 'PUT', cuerpo: { ...datos, version } })
}

/** RF33 (DEC-210): otro estado, con la versión leída; 409 ATRACTIVO_MODIFICADO (`versionActual`). */
export function cambiarEstadoAtractivo(id: string, estadoNuevo: EstadoOperativo, version: number): Promise<AtractivoGestion> {
  return solicitar<AtractivoGestion>(`${RUTA}/${encodeURIComponent(id)}/estado`, {
    method: 'POST',
    cuerpo: { estadoNuevo, version },
  })
}
