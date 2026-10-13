import { solicitar } from '../../shared/api/httpClient'
import type { Categoria, ServicioIncluido } from './catalogoApi'

export interface Rango {
  minimo: number
  maximo: number
}

/**
 * Contrato de GET /api/v1/catalogo/filtros?categoria= (ms-catalog: OpcionesFiltroController). Son los valores que
 * tienen hoy los servicios visibles de la categoría, así que cada opción devuelve resultados. Solo llegan los campos
 * de esa categoría; los rangos faltan si la categoría no tiene servicios visibles.
 */
export interface OpcionesFiltro {
  categoria: Categoria
  serviciosIncluidos?: ServicioIncluido[]
  tiposHabitacion?: string[]
  huespedes?: Rango
  opcionVegetariana?: boolean
  tiposCocina?: string[]
  nivelesDificultad?: string[]
  duracionHoras?: Rango
  tiposVehiculoEquipo?: string[]
}

export function consultarOpcionesFiltro(categoria: Categoria): Promise<OpcionesFiltro> {
  return solicitar<OpcionesFiltro>(`/api/v1/catalogo/filtros?categoria=${encodeURIComponent(categoria)}`)
}
