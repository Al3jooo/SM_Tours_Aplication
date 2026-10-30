import { act, fireEvent, render, screen } from '@testing-library/react'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { mapaVivo } from '../../../test/leafletSimulado'
import { SelectorUbicacion, type Coordenadas } from '../componentes/SelectorUbicacion'

/** El selector con su estado, como lo usa WF-08. */
function Selector({ inicial = { latitud: '', longitud: '' } }: { inicial?: Coordenadas }) {
  const [valor, setValor] = useState(inicial)
  return <SelectorUbicacion valor={valor} errores={{}} onCambiar={setValor} />
}

describe('SelectorUbicacion (WF-08, DEC-165)', () => {
  it('empieza en Santa Marta, sin pin, con las teselas de OpenStreetMap y su atribución', () => {
    render(<Selector />)

    const mapa = mapaVivo()
    expect(mapa.opciones.center).toEqual([11.242, -74.2117])
    expect(mapa.opciones.maxZoom).toBe(19)
    expect(mapa.teselas()[0].url).toBe('https://tile.openstreetmap.org/{z}/{x}/{y}.png')
    expect(mapa.teselas()[0].opciones.attribution).toContain('OpenStreetMap')
    expect(mapa.marcadores()).toHaveLength(0)
    expect(screen.getByRole('region', { name: 'Mapa para ubicar el servicio' })).toHaveAttribute('tabindex', '0')
  })

  it('un clic pone el pin y llena los campos; el pin se arrastra y los campos lo siguen', () => {
    render(<Selector />)
    const mapa = mapaVivo()

    act(() => {
      mapa.fire('click', { latlng: { lat: 11.2408123456, lng: -74.2110987654 } })
    })
    expect(screen.getByLabelText('Latitud')).toHaveValue('11.240812')
    expect(screen.getByLabelText('Longitud')).toHaveValue('-74.211099')
    const [pin] = mapa.marcadores()
    expect(pin.opciones.draggable).toBe(true)
    expect(pin.getElement()).toHaveAttribute('aria-label', 'Ubicación del servicio')
    // Un cambio desde el mapa no lo vuelve a centrar.
    expect(mapa.vistas).toHaveLength(0)

    act(() => {
      pin.arrastrarA(11.25, -74.2)
    })
    expect(screen.getByLabelText('Latitud')).toHaveValue('11.250000')
    expect(screen.getByLabelText('Longitud')).toHaveValue('-74.200000')
  })

  it('sin ratón: los campos numéricos ponen el pin y centran el mapa; también «Usar el centro de Santa Marta»', () => {
    render(<Selector />)
    const mapa = mapaVivo()

    fireEvent.change(screen.getByLabelText('Latitud'), { target: { value: '11.1432' } })
    expect(mapa.marcadores()).toHaveLength(0)
    fireEvent.change(screen.getByLabelText('Longitud'), { target: { value: '-74.1121' } })
    expect(mapa.marcadores()[0].posicion).toEqual([11.1432, -74.1121])
    expect(mapa.vistas.at(-1)?.centro).toEqual([11.1432, -74.1121])

    // Una coordenada fuera de rango no dibuja el pin.
    fireEvent.change(screen.getByLabelText('Latitud'), { target: { value: '95' } })
    expect(mapa.marcadores()).toHaveLength(0)

    fireEvent.click(screen.getByRole('button', { name: 'Usar el centro de Santa Marta' }))
    expect(screen.getByLabelText('Latitud')).toHaveValue('11.242000')
    expect(screen.getByLabelText('Longitud')).toHaveValue('-74.211700')
    expect(mapa.marcadores()[0].posicion).toEqual([11.242, -74.2117])
  })
})
