import { useCallback, useEffect, useState } from 'react'
import { ApiError, servicioNoDisponible, solicitar } from '../../../shared/api/httpClient'
import type { CondicionAccesibilidad } from '../catalogoApi'

/**
 * Contrato de GET /api/v1/catalogo/servicios/{id} (ms-catalog, RespuestaServicio): solo servicios Publicados de
 * prestadores activos; si no, 404 OFERTA_NO_DISPONIBLE. Los precios llegan como número o texto decimal (BigDecimal).
 */
export interface UnidadAlojamiento {
  idUnidad: string
  nombre: string
  tipoHabitacion: string
  numHabitaciones: number
  capacidadMaxHuespedes: number
  /** Por noche y por habitación. */
  precioNoche: number | string
}

interface BaseDetalle {
  id: string
  idPrestador: string
  nombreComercial: string
  descripcion: string | null
  /** La primera es la portada (RF48). */
  imagenes: string[]
  /** Precio «desde» (RF37); en un alojamiento, el menor precio por noche de sus unidades. */
  precioBase: number | string | null
  moneda: string
  /** Contacto de atención al público del servicio (RF09, RF43); null si no hay. */
  contacto: { telefono: string | null; correo: string | null } | null
  /** Las declaradas (RF27), en el orden de RF27; vacía si no hay. */
  condicionesAccesibilidad: CondicionAccesibilidad[]
  /** Días y horarios como texto libre (RF58); null si no hay. */
  horarioAtencion: string | null
}

export type DetalleServicio = BaseDetalle &
  (
    | {
        categoria: 'Alojamiento'
        detalleCategoria: { serviciosIncluidos: string[]; unidades: UnidadAlojamiento[] }
      }
    | {
        categoria: 'Tour'
        detalleCategoria: {
          puntoEncuentro: string
          duracionHoras: number
          nivelDificultad: string | null
          aforoMaxGrupo: number
        }
      }
    | {
        categoria: 'Gastronomia'
        detalleCategoria: {
          tipoCocina: string
          aforoMaxTurno: number
          menuVegetariano: boolean | null
          rangoPrecioPlato: string | null
        }
      }
    | {
        categoria: 'AlquilerEquipo'
        detalleCategoria: { tipoVehiculoEquipo: string; unidadesDisponibles: number; condicionesAlquiler: string | null }
      }
  )

export function consultarServicio(id: string): Promise<DetalleServicio> {
  return solicitar<DetalleServicio>(`/api/v1/catalogo/servicios/${encodeURIComponent(id)}`, { publica: true })
}

export type ErrorDetalle = 'noEncontrado' | 'noDisponible' | 'red' | 'otro'

export type EstadoDetalle =
  | { estado: 'cargando' }
  | { estado: 'ok'; servicio: DetalleServicio }
  | { estado: 'error'; error: ErrorDetalle }

function clasificar(error: unknown): ErrorDetalle {
  if (!(error instanceof ApiError)) return 'otro'
  if (error.codigo === 'OFERTA_NO_DISPONIBLE' || error.status === 404) return 'noEncontrado'
  if (servicioNoDisponible(error)) return 'noDisponible'
  if (error.codigo === 'ERROR_RED') return 'red'
  return 'otro'
}

/** Detalle de un servicio (WF-06 y WF-10); `recargar` lo vuelve a pedir (Reintentar, UNIDAD_NO_DISPONIBLE). */
export function useDetalleServicio(id: string) {
  const [estado, setEstado] = useState<EstadoDetalle>({ estado: 'cargando' })
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vigente = true
    setEstado((actual) => (actual.estado === 'ok' && actual.servicio.id === id ? actual : { estado: 'cargando' }))
    consultarServicio(id)
      .then((servicio) => {
        if (vigente) setEstado({ estado: 'ok', servicio })
      })
      .catch((error: unknown) => {
        if (vigente) setEstado({ estado: 'error', error: clasificar(error) })
      })
    return () => {
      vigente = false
    }
  }, [id, intento])

  const recargar = useCallback(() => setIntento((n) => n + 1), [])
  return { ...estado, recargar } as EstadoDetalle & { recargar: () => void }
}
