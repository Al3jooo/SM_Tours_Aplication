import { useCallback, useState } from 'react'
import type { Ubicacion } from './catalogoApi'
import { estaLejosDeSantaMarta } from './geo'

export type MotivoErrorUbicacion = 'denegada' | 'noDisponible' | 'tiempoAgotado'

export type EstadoUbicacion =
  | { estado: 'inactiva' }
  | { estado: 'solicitando' }
  | { estado: 'disponible'; ubicacion: Ubicacion; lejos: boolean }
  | { estado: 'error'; motivo: MotivoErrorUbicacion }
  | { estado: 'noSoportada' }

/** Ubicación aproximada: basta para ordenar por cercanía y ahorra batería; acepta una lectura de hasta 1 minuto. */
const OPCIONES: PositionOptions = { enableHighAccuracy: false, timeout: 10_000, maximumAge: 60_000 }

function motivo(error: GeolocationPositionError): MotivoErrorUbicacion {
  if (error.code === 1) return 'denegada'
  if (error.code === 3) return 'tiempoAgotado'
  return 'noDisponible'
}

const geolocalizacionSoportada = () => typeof navigator !== 'undefined' && 'geolocation' in navigator

/**
 * Ubicación de quien explora, solo a pedido («Centrar en mi ubicación») y solo en memoria: no se guarda en el
 * almacenamiento del navegador ni en la URL, y se pierde al recargar o salir de la página.
 *
 * `centrado` cuenta las veces que hay que centrar el mapa: sube al obtener la ubicación y en cada pulsación posterior,
 * que ya no vuelve a pedirla.
 */
export function useUbicacion() {
  const [estado, setEstado] = useState<EstadoUbicacion>(() =>
    geolocalizacionSoportada() ? { estado: 'inactiva' } : { estado: 'noSoportada' },
  )
  const [centrado, setCentrado] = useState(0)

  const centrar = useCallback(() => {
    if (estado.estado === 'disponible') {
      setCentrado((n) => n + 1)
      return
    }
    if (estado.estado === 'solicitando' || estado.estado === 'noSoportada') return
    setEstado({ estado: 'solicitando' })
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        const ubicacion = { latitud: coords.latitude, longitud: coords.longitude }
        setEstado({ estado: 'disponible', ubicacion, lejos: estaLejosDeSantaMarta(ubicacion) })
        setCentrado((n) => n + 1)
      },
      (error) => setEstado({ estado: 'error', motivo: motivo(error) }),
      OPCIONES,
    )
  }, [estado.estado])

  return { ...estado, centrado, centrar }
}
