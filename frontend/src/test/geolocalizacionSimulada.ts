import { vi } from 'vitest'

/**
 * navigator.geolocation simulado (jsdom no lo trae: sin simularlo, el navegador «no soporta» la ubicación).
 * Responde en una microtarea, como el navegador, con la posición o con el error del código dado
 * (1 permiso denegado, 2 posición no disponible, 3 tiempo agotado). setup.ts lo restaura después de cada prueba.
 */
export function simularGeolocalizacion(respuesta: { latitud: number; longitud: number } | { codigo: 1 | 2 | 3 }) {
  const getCurrentPosition = vi.fn(
    (exito: PositionCallback, error?: PositionErrorCallback | null, _opciones?: PositionOptions) => {
      queueMicrotask(() => {
        if ('codigo' in respuesta) {
          error?.({
            code: respuesta.codigo,
            message: '',
            PERMISSION_DENIED: 1,
            POSITION_UNAVAILABLE: 2,
            TIMEOUT: 3,
          } as GeolocationPositionError)
        } else {
          exito({
            coords: { latitude: respuesta.latitud, longitude: respuesta.longitud, accuracy: 30 },
            timestamp: Date.now(),
          } as GeolocationPosition)
        }
      })
    },
  )
  Object.defineProperty(navigator, 'geolocation', {
    configurable: true,
    value: { getCurrentPosition, watchPosition: vi.fn(), clearWatch: vi.fn() },
  })
  return getCurrentPosition
}

export function restaurarGeolocalizacion() {
  if (Object.getOwnPropertyDescriptor(navigator, 'geolocation')) {
    delete (navigator as { geolocation?: Geolocation }).geolocation
  }
}
