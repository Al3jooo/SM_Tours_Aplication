import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterEach, beforeEach, vi } from 'vitest'
import i18n from '../i18n'
import { restaurarGeolocalizacion } from './geolocalizacionSimulada'
import { reiniciarLeafletSimulado } from './leafletSimulado'

// jsdom no dibuja mapas: todas las pruebas usan el Leaflet simulado (ver leafletSimulado.ts).
vi.mock('leaflet', async () => (await import('./leafletSimulado')).moduloLeaflet)

// /explorar se carga de forma diferida (React.lazy): la primera vez en cada archivo, Vitest transforma el módulo
// dentro de la espera de findBy/waitFor, y con las pruebas en paralelo eso puede pasar de 1 s (el valor por defecto).
configure({ asyncUtilTimeout: 3000 })

beforeEach(async () => {
  sessionStorage.clear()
  localStorage.clear()
  reiniciarLeafletSimulado()
  await i18n.changeLanguage('es')
})

afterEach(() => {
  cleanup()
  restaurarGeolocalizacion()
})
