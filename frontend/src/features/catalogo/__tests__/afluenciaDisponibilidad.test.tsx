import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import i18n from '../../../i18n'
import {
  CASCADA_CERRADA,
  HOTEL,
  TOUR_CAFE,
  paginaOferta,
  respuestaJson,
  simularFetch,
} from '../../../test/fetchSimulado'
import { renderApp } from '../../../test/renderApp'
import type { ResultadoOferta } from '../catalogoApi'
import { TarjetaOferta } from '../componentes/TarjetaOferta'

/**
 * Según el SRS, la etiqueta depende del tipo de oferta y no de la página: los atractivos muestran el nivel de
 * afluencia (RF39) y los servicios la disponibilidad de cupos (RF11). «Aforo» no es el término del SRS.
 * `datosDe` devuelve la lista de datos de la tarjeta (calificación, afluencia o disponibilidad, distancia).
 */
const datosDe = (articulo: HTMLElement) => articulo.querySelector('dl')!

describe('Afluencia (RF39) y Disponibilidad (RF11) en las tarjetas', () => {
  it.each([
    ['destacado', 'servicio', HOTEL, 'Disponibilidad', 'Availability'],
    ['destacado', 'atractivo', CASCADA_CERRADA, 'Afluencia', 'Crowd level'],
    ['lista', 'servicio', HOTEL, 'Disponibilidad', 'Availability'],
    ['lista', 'atractivo', CASCADA_CERRADA, 'Afluencia', 'Crowd level'],
  ] as const)('variante %s, %s: muestra «%s» en ES y «%s» en EN', async (variante, _tipo, resultado, es, en) => {
    render(<TarjetaOferta resultado={resultado as unknown as ResultadoOferta} variante={variante} />)
    const datos = datosDe(screen.getByRole('article'))

    expect(within(datos).getByText(es)).toBeInTheDocument()
    expect(within(datos).queryByText(/Aforo/)).not.toBeInTheDocument()

    await i18n.changeLanguage('en')
    expect(await within(datos).findByText(en)).toBeInTheDocument()
    expect(within(datos).queryByText(/Occupancy/)).not.toBeInTheDocument()
  })

  it('en /explorar: el servicio muestra «Disponibilidad» y el atractivo «Afluencia»', async () => {
    simularFetch({ 'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([HOTEL, CASCADA_CERRADA])) })
    renderApp('/explorar')

    const servicio = await screen.findByRole('article', { name: 'Hotel Casa Tayrona' })
    const atractivo = screen.getByRole('article', { name: 'Cascada El Salto' })
    expect(within(servicio).getByText('Disponibilidad')).toBeInTheDocument()
    expect(within(servicio).queryByText('Afluencia')).not.toBeInTheDocument()
    expect(within(atractivo).getByText('Afluencia')).toBeInTheDocument()
    expect(within(atractivo).queryByText('Disponibilidad')).not.toBeInTheDocument()
  })

  it('en el inicio: el servicio destacado muestra «Disponibilidad» (los destacados solo incluyen servicios)', async () => {
    simularFetch({
      'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([TOUR_CAFE, CASCADA_CERRADA], { tamano: 3 })),
    })
    renderApp('/')

    const servicio = await screen.findByRole('article', { name: 'Tour de Café en Minca' })
    expect(within(servicio).getByText('Disponibilidad')).toBeInTheDocument()
    expect(screen.queryByText(/Aforo/)).not.toBeInTheDocument()
  })
})
