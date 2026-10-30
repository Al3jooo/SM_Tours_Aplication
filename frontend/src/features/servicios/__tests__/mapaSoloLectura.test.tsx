import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { mapaVivo } from '../../../test/leafletSimulado'
import { MapaSoloLectura } from '../MapaSoloLectura'

describe('MapaSoloLectura (WF-19, DEC-176)', () => {
  it('centra el mapa en el pin, con las teselas de OpenStreetMap, su atribución y zoom máximo 19', () => {
    render(<MapaSoloLectura latitud={11.1432} longitud={-74.1121} nombre="Tour de Café en Minca" />)

    const mapa = mapaVivo()
    expect(mapa.opciones.maxZoom).toBe(19)
    expect(mapa.teselas()[0].url).toBe('https://tile.openstreetmap.org/{z}/{x}/{y}.png')
    expect(mapa.teselas()[0].opciones.attribution).toContain('OpenStreetMap')
    expect(mapa.vistas.at(-1)?.centro).toEqual([11.1432, -74.1121])
    expect(screen.getByRole('region', { name: 'Ubicación de Tour de Café en Minca en el mapa' })).toHaveAttribute(
      'tabindex',
      '0',
    )
  })

  it('el pin no se arrastra y un clic en el mapa no lo mueve', () => {
    render(<MapaSoloLectura latitud={11.1432} longitud={-74.1121} nombre="Tour de Café en Minca" />)

    const mapa = mapaVivo()
    const [pin] = mapa.marcadores()
    expect(pin.posicion).toEqual([11.1432, -74.1121])
    expect(pin.opciones.draggable).toBeFalsy()
    expect(() => pin.arrastrarA(11, -74)).toThrow()
    mapa.fire('click', { latlng: { lat: 11.3, lng: -74.0 } })
    expect(mapa.marcadores()).toHaveLength(1)
    expect(mapa.marcadores()[0].posicion).toEqual([11.1432, -74.1121])
  })

  it('al desmontarse elimina el mapa', () => {
    const { unmount } = render(<MapaSoloLectura latitud={11.1432} longitud={-74.1121} nombre="Tour" />)
    const mapa = mapaVivo()
    unmount()
    expect(mapa.eliminado).toBe(true)
  })
})
