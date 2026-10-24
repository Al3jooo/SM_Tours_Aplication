import { solicitar } from '../../shared/api/httpClient'
import {
  agregarMasFiltros,
  soloDeCategoria,
  type Categoria,
  type CondicionAccesibilidad,
  type CriteriosBusqueda,
  type MasFiltros,
  type Orden,
  type ServicioIncluido,
} from './catalogoApi'

/**
 * Contrato del historial de búsquedas de ms-catalog (docs/sad/api/ms-catalog.md, «Historial de búsquedas»; RF60,
 * DEC-56, DEC-88). Solo el rol Turista: las dos llamadas llevan el token; la búsqueda pública (/oferta) sigue sin él.
 */

const RUTA = '/api/v1/catalogo/historial'

/** Criterios guardados: todos los campos, con null (o lista vacía) en los que no se usaron. */
export interface CriteriosHistorial {
  texto: string | null
  categoria: Categoria | null
  precioMax: number | null
  usarUbicacion: boolean
  distanciaKm: number | null
  accesibilidad: CondicionAccesibilidad[]
  orden: Orden | null
  serviciosIncluidos: ServicioIncluido[]
  huespedes: number | null
  tipoHabitacion: string | null
  menuVegetariano: boolean | null
  tipoCocina: string | null
  nivelDificultad: string | null
  duracionMaxHoras: number | null
  tipoVehiculoEquipo: string | null
}

export interface BusquedaReciente {
  id: string
  fecha: string
  criterios: CriteriosHistorial
}

/** Cuerpo de POST /historial: los nombres de GET /oferta y `usarUbicacion`; nunca lat, lng, pagina ni tamano. */
export type CuerpoHistorial = Record<string, string | number | boolean | string[]>

/**
 * Los criterios de una búsqueda ya hecha, como los guarda el historial (DEC-95). Sin ubicación no van la distancia ni
 * el orden por distancia (la misma regla de `consultaApi`: el servidor respondería 400 CRITERIOS_INVALIDOS). Devuelve
 * null si no hay criterios (solo el orden, o nada): el servidor no la guardaría.
 */
export function cuerpoHistorial(criterios: CriteriosBusqueda, usarUbicacion: boolean): CuerpoHistorial | null {
  const cuerpo: CuerpoHistorial = {}
  if (criterios.texto) cuerpo.texto = criterios.texto
  if (criterios.categoria) cuerpo.categoria = criterios.categoria
  const masFiltros = new URLSearchParams()
  agregarMasFiltros(masFiltros, soloDeCategoria(criterios.masFiltros, criterios.categoria))
  for (const clave of new Set(masFiltros.keys())) {
    const valores = masFiltros.getAll(clave)
    if (clave === 'serviciosIncluidos') cuerpo[clave] = valores
    else if (clave === 'menuVegetariano') cuerpo[clave] = true
    else if (clave === 'huespedes' || clave === 'duracionMaxHoras') cuerpo[clave] = Number(valores[0])
    else cuerpo[clave] = valores[0]
  }
  if (criterios.precioMax !== undefined) cuerpo.precioMax = criterios.precioMax
  if (usarUbicacion && criterios.distanciaKm !== undefined) cuerpo.distanciaKm = criterios.distanciaKm
  if (criterios.accesibilidad.length > 0) cuerpo.accesibilidad = [...criterios.accesibilidad]
  const sinOrden = Object.keys(cuerpo).length === 0 && !usarUbicacion
  if (criterios.orden && (criterios.orden !== 'Distancia' || usarUbicacion)) cuerpo.orden = criterios.orden
  if (sinOrden) return null
  cuerpo.usarUbicacion = usarUbicacion
  return cuerpo
}

/** Los criterios guardados como criterios de la pantalla (página 1), para repetir la búsqueda con un clic. */
export function criteriosDesdeHistorial(guardados: CriteriosHistorial): CriteriosBusqueda {
  const masFiltros: MasFiltros = {
    serviciosIncluidos: guardados.serviciosIncluidos,
    huespedes: guardados.huespedes ?? undefined,
    tipoHabitacion: guardados.tipoHabitacion ?? undefined,
    menuVegetariano: guardados.menuVegetariano || undefined,
    tipoCocina: guardados.tipoCocina ?? undefined,
    nivelDificultad: guardados.nivelDificultad ?? undefined,
    duracionMaxHoras: guardados.duracionMaxHoras ?? undefined,
    tipoVehiculoEquipo: guardados.tipoVehiculoEquipo ?? undefined,
  }
  const categoria = guardados.categoria ?? undefined
  return {
    texto: guardados.texto ?? undefined,
    categoria,
    masFiltros: soloDeCategoria(masFiltros, categoria),
    precioMax: guardados.precioMax ?? undefined,
    distanciaKm: guardados.distanciaKm ?? undefined,
    accesibilidad: guardados.accesibilidad,
    orden: guardados.orden ?? undefined,
    pagina: 1,
  }
}

/** 204 siempre que el cuerpo sea válido (guarda, actualiza la fecha o la ignora). */
export async function registrarBusqueda(cuerpo: CuerpoHistorial): Promise<void> {
  await solicitar<void>(RUTA, { method: 'POST', cuerpo })
}

/** Como máximo 10, la más reciente primero. */
export function consultarHistorial(): Promise<{ elementos: BusquedaReciente[] }> {
  return solicitar<{ elementos: BusquedaReciente[] }>(RUTA)
}
