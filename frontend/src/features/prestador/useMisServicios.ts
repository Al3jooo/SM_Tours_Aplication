import { useCallback, useEffect, useState } from 'react'
import { ApiError, ERROR_RED, servicioNoDisponible } from '../../shared/api/httpClient'
import type { CriteriosMisServicios } from './criteriosMisServicios'
import { consultarMiServicio, listarMisServicios, type PaginaServicios, type ServicioGestion } from './serviciosPrestadorApi'

/** sinPermiso: 403 ACCESO_DENEGADO. Un 401 ya cerró la sesión (httpClient). */
export type ErrorMisServicios = 'sinPermiso' | 'noEncontrado' | 'noDisponible' | 'red' | 'otro'

export function clasificarError(error: unknown): ErrorMisServicios {
  if (!(error instanceof ApiError)) return 'otro'
  if (error.status === 403 && error.codigo === 'ACCESO_DENEGADO') return 'sinPermiso'
  if (error.status === 404) return 'noEncontrado'
  if (servicioNoDisponible(error)) return 'noDisponible'
  if (error.codigo === ERROR_RED) return 'red'
  return 'otro'
}

export type EstadoCarga<T> = { estado: 'cargando' } | { estado: 'ok'; datos: T } | { estado: 'error'; error: ErrorMisServicios }

/** Listado de «Mis servicios» (WF-18) con los criterios de la URL; `recargar` es «Reintentar». */
export function useMisServicios(criterios: CriteriosMisServicios) {
  const [estado, setEstado] = useState<EstadoCarga<PaginaServicios>>({ estado: 'cargando' })
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vigente = true
    setEstado({ estado: 'cargando' })
    listarMisServicios(criterios.estado, criterios.pagina)
      .then((datos) => {
        if (vigente) setEstado({ estado: 'ok', datos })
      })
      .catch((error: unknown) => {
        if (vigente) setEstado({ estado: 'error', error: clasificarError(error) })
      })
    return () => {
      vigente = false
    }
  }, [criterios.estado, criterios.pagina, intento])

  const recargar = useCallback(() => setIntento((n) => n + 1), [])
  return { ...estado, recargar } as EstadoCarga<PaginaServicios> & { recargar: () => void }
}

/** Un servicio propio (detalle de WF-18). */
export function useMiServicio(id: string) {
  const [estado, setEstado] = useState<EstadoCarga<ServicioGestion>>({ estado: 'cargando' })
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vigente = true
    setEstado({ estado: 'cargando' })
    consultarMiServicio(id)
      .then((datos) => {
        if (vigente) setEstado({ estado: 'ok', datos })
      })
      .catch((error: unknown) => {
        if (vigente) setEstado({ estado: 'error', error: clasificarError(error) })
      })
    return () => {
      vigente = false
    }
  }, [id, intento])

  const recargar = useCallback(() => setIntento((n) => n + 1), [])
  return { ...estado, recargar } as EstadoCarga<ServicioGestion> & { recargar: () => void }
}
