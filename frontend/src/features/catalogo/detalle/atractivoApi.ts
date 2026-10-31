import { useCallback, useEffect, useState } from 'react'
import { ApiError, servicioNoDisponible, solicitar } from '../../../shared/api/httpClient'
import type { CondicionAccesibilidad, EstadoOperativo } from '../catalogoApi'
import type { ErrorDetalle } from './detalleApi'

/**
 * Ficha pública de un atractivo (DEC-217, DEC-224): `GET /api/v1/catalogo/atractivos/{id}`, sin token, visible mientras
 * exista (DEC-87). No trae el Gestor, la versión ni las fechas. Las imágenes son rutas que el navegador pide con <img>
 * (`/api/v1/catalogo/imagenes/{id}` o, en la semilla, `/imagenes/catalogo/*.webp`).
 */
export interface DetalleAtractivo {
  id: string
  nombre: string
  /** `Naturaleza` o `Cultura` (DEC-214); null solo en un atractivo anterior a M012 sin categoría. */
  categoria: string | null
  descripcion: string | null
  coordenadas: { longitud: number; latitud: number }
  imagenes: string[]
  /** RF07. */
  limiteCapacidadCarga: number
  /** RF33. */
  estadoOperativo: EstadoOperativo | null
  /** RF24. */
  normasAmbientales: string | null
  /** RF58. */
  horarioAtencion: string | null
  /** RF27, en el orden del SRS. */
  condicionesAccesibilidad: CondicionAccesibilidad[]
}

export function consultarAtractivo(id: string): Promise<DetalleAtractivo> {
  return solicitar<DetalleAtractivo>(`/api/v1/catalogo/atractivos/${encodeURIComponent(id)}`, { publica: true })
}

export type EstadoDetalleAtractivo =
  | { estado: 'cargando' }
  | { estado: 'ok'; atractivo: DetalleAtractivo }
  | { estado: 'error'; error: ErrorDetalle }

/** El 404 es ATRACTIVO_NO_ENCONTRADO (enmienda de DEC-217): un id inválido o inexistente. */
function clasificar(error: unknown): ErrorDetalle {
  if (!(error instanceof ApiError)) return 'otro'
  if (error.codigo === 'ATRACTIVO_NO_ENCONTRADO' || error.status === 404) return 'noEncontrado'
  if (servicioNoDisponible(error)) return 'noDisponible'
  if (error.codigo === 'ERROR_RED') return 'red'
  return 'otro'
}

/** Ficha de un atractivo; `recargar` es «Reintentar». */
export function useDetalleAtractivo(id: string) {
  const [estado, setEstado] = useState<EstadoDetalleAtractivo>({ estado: 'cargando' })
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vigente = true
    setEstado({ estado: 'cargando' })
    consultarAtractivo(id)
      .then((atractivo) => {
        if (vigente) setEstado({ estado: 'ok', atractivo })
      })
      .catch((error: unknown) => {
        if (vigente) setEstado({ estado: 'error', error: clasificar(error) })
      })
    return () => {
      vigente = false
    }
  }, [id, intento])

  const recargar = useCallback(() => setIntento((n) => n + 1), [])
  return { ...estado, recargar } as EstadoDetalleAtractivo & { recargar: () => void }
}
