import { useCallback, useEffect, useState } from 'react'
import { ApiError, servicioDelProblema, servicioNoDisponible } from '../../shared/api/httpClient'
import { consultarDisponibilidad, type Disponibilidad } from './reservasApi'

/**
 * `catalogoNoDisponible`: el 503 de ms-booking con `servicio: ms-catalog` (falta materializar la oferta y el catálogo
 * no responde, A2); no es el sistema de reservas el que falla (O14, DEC-171).
 */
export type ErrorDisponibilidad = 'noDisponible' | 'catalogoNoDisponible' | 'red' | 'otro'

export type EstadoDisponibilidad =
  | { estado: 'inactivo' }
  | { estado: 'cargando' }
  | { estado: 'ok'; datos: Disponibilidad; consultadaEn: Date }
  | { estado: 'error'; error: ErrorDisponibilidad }

function clasificar(error: unknown): ErrorDisponibilidad {
  if (!(error instanceof ApiError)) return 'otro'
  if (servicioNoDisponible(error)) return servicioDelProblema(error) === 'ms-catalog' ? 'catalogoNoDisponible' : 'noDisponible'
  if (error.codigo === 'ERROR_RED') return 'red'
  return 'otro'
}

/**
 * Disponibilidad «en tiempo real» de WF-10 (RF11): se consulta al verificar, al cambiar el rango y al recargar
 * (por ejemplo, después de un CUPO_AGOTADO). Sin rango, no consulta.
 */
export function useDisponibilidad(servicio: string, rango: { desde: string; hasta: string } | null) {
  const [estado, setEstado] = useState<EstadoDisponibilidad>({ estado: 'inactivo' })
  const [intento, setIntento] = useState(0)
  const desde = rango?.desde
  const hasta = rango?.hasta

  useEffect(() => {
    if (!desde || !hasta) {
      setEstado({ estado: 'inactivo' })
      return
    }
    let vigente = true
    setEstado({ estado: 'cargando' })
    consultarDisponibilidad(servicio, desde, hasta)
      .then((datos) => {
        if (vigente) setEstado({ estado: 'ok', datos, consultadaEn: new Date() })
      })
      .catch((error: unknown) => {
        if (vigente) setEstado({ estado: 'error', error: clasificar(error) })
      })
    return () => {
      vigente = false
    }
  }, [servicio, desde, hasta, intento])

  const recargar = useCallback(() => setIntento((n) => n + 1), [])
  return { ...estado, recargar } as EstadoDisponibilidad & { recargar: () => void }
}
