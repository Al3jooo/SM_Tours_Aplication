import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterEach, beforeEach, vi } from 'vitest'
import i18n from '../i18n'
import { restaurarGeolocalizacion } from './geolocalizacionSimulada'
import { reiniciarLeafletSimulado } from './leafletSimulado'

// jsdom no dibuja mapas: todas las pruebas usan el Leaflet simulado (ver leafletSimulado.ts).
vi.mock('leaflet', async () => (await import('./leafletSimulado')).moduloLeaflet)

// /explorar se carga de forma diferida (React.lazy): la primera vez en cada archivo, Vitest transforma el módulo
// dentro de la espera de findBy/waitFor, y con las pruebas en paralelo (o con Docker Compose corriendo) eso puede
// pasar de 1 s (el valor por defecto).
configure({ asyncUtilTimeout: 5000 })

/**
 * Guardia de red: ninguna prueba sale a la red. Cada prueba empieza con un fetch que registra la llamada y la
 * rechaza; las que necesitan respuestas lo reemplazan con simularFetch (fetchSimulado.ts). El rechazo lo atrapa el
 * código de la app (p. ej. useBusquedaOferta), así que la prueba falla en afterEach con la URL pedida.
 */
const peticionesSinSimular: string[] = []

beforeEach(async () => {
  sessionStorage.clear()
  localStorage.clear()
  reiniciarLeafletSimulado()
  vi.stubGlobal(
    'fetch',
    vi.fn(async (entrada: RequestInfo | URL) => {
      peticionesSinSimular.push(String(entrada))
      throw new TypeError(`Petición de red sin simular: ${String(entrada)}`)
    }),
  )
  await i18n.changeLanguage('es')
})

afterEach(() => {
  cleanup()
  restaurarGeolocalizacion()
  // Restaura el fetch original (también el de simularFetch): nada pasa de una prueba a otra.
  vi.unstubAllGlobals()
  const pendientes = peticionesSinSimular.splice(0)
  if (pendientes.length > 0) {
    throw new Error(`Petición de red sin simular: ${pendientes.join(', ')}`)
  }
})
