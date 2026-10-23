import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterEach, beforeEach, vi } from 'vitest'
import i18n from '../i18n'
import { esAvisosDelBanner, respuestaAvisosVacia } from './fetchSimulado'
import { restaurarGeolocalizacion } from './geolocalizacionSimulada'
import { reiniciarLeafletSimulado } from './leafletSimulado'

// jsdom no dibuja mapas: todas las pruebas usan el Leaflet simulado (ver leafletSimulado.ts).
vi.mock('leaflet', async () => (await import('./leafletSimulado')).moduloLeaflet)

/**
 * jsdom tiene <dialog> (con `open`), pero no showModal() ni close(). Polyfill mínimo con lo que usa la app
 * (DialogoCancelar): showModal() lo abre; close() lo cierra y dispara «close»; Esc dentro del diálogo dispara «cancel»
 * (cancelable) y, si nadie lo impide, lo cierra, como el navegador. El foco atrapado lo da el navegador real: se
 * verifica en el recorrido manual.
 */
if (!HTMLDialogElement.prototype.showModal) {
  const alPulsarEsc = function (this: HTMLDialogElement, evento: KeyboardEvent) {
    if (evento.key !== 'Escape' || !this.open) return
    const cancelar = new Event('cancel', { cancelable: true })
    if (this.dispatchEvent(cancelar)) this.close()
  }
  HTMLDialogElement.prototype.showModal = function (this: HTMLDialogElement) {
    if (this.open) throw new DOMException('El diálogo ya está abierto', 'InvalidStateError')
    this.open = true
    this.addEventListener('keydown', alPulsarEsc)
  }
  HTMLDialogElement.prototype.close = function (this: HTMLDialogElement) {
    if (!this.open) return
    this.open = false
    this.removeEventListener('keydown', alPulsarEsc)
    this.dispatchEvent(new Event('close'))
  }
}

// /explorar se carga de forma diferida (React.lazy): la primera vez en cada archivo, Vitest transforma el módulo
// dentro de la espera de findBy/waitFor, y con las pruebas en paralelo (o con Docker Compose corriendo) eso puede
// pasar de 1 s (el valor por defecto).
configure({ asyncUtilTimeout: 5000 })

/**
 * Guardia de red: ninguna prueba sale a la red. Cada prueba empieza con un fetch que registra la llamada y la
 * rechaza; las que necesitan respuestas lo reemplazan con simularFetch (fetchSimulado.ts). El rechazo lo atrapa el
 * código de la app (p. ej. useBusquedaOferta), así que la prueba falla en afterEach con la URL pedida.
 *
 * Única excepción (DEC-78): `GET /api/v1/catalogo/avisos`, exacta y sin consulta, responde una lista vacía. La pide el
 * banner común (DEC-72) en todas las pantallas de la plataforma; sin avisos, el banner no se muestra.
 */
const peticionesSinSimular: string[] = []

/** Devuelve y descarta las peticiones sin simular de la prueba actual (solo para probar la propia guardia). */
export function descartarPeticionesSinSimular(): string[] {
  return peticionesSinSimular.splice(0)
}

beforeEach(async () => {
  sessionStorage.clear()
  localStorage.clear()
  reiniciarLeafletSimulado()
  vi.stubGlobal(
    'fetch',
    vi.fn(async (entrada: RequestInfo | URL, init?: RequestInit) => {
      if (esAvisosDelBanner(String(entrada), init?.method)) return respuestaAvisosVacia()
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
