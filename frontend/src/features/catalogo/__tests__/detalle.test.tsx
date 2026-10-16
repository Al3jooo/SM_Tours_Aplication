import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  DETALLE_HOTEL,
  DETALLE_RESTAURANTE,
  DETALLE_TOUR,
  HOTEL,
  paginaOferta,
  respuestaJson,
  respuestaProblema,
  simularFetch,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

const DETALLE = 'GET /api/v1/catalogo/servicios'

describe('Detalle de un servicio (WF-06, versión mínima)', () => {
  it('un alojamiento muestra sus datos, sus unidades y «Ver disponibilidad y reservar»', async () => {
    simularFetch({ [`${DETALLE}/s-hotel`]: () => respuestaJson(200, DETALLE_HOTEL) })
    renderApp('/servicios/s-hotel')

    expect(await screen.findByRole('heading', { level: 1, name: 'Hotel Casa Tayrona' })).toBeInTheDocument()
    const ficha = screen.getByRole('article', { name: 'Hotel Casa Tayrona' })
    expect(within(ficha).getByText('Alojamiento')).toHaveClass('detalle__categoria')
    expect(screen.getByText(/^Desde \$\s150\.000$/)).toBeInTheDocument()
    expect(screen.getByText('Alojamiento boutique cerca del Parque de los Novios.')).toBeInTheDocument()

    const incluidos = screen.getByRole('region', { name: 'Servicios incluidos' })
    expect(within(incluidos).getAllByRole('listitem').map((item) => item.textContent)).toEqual(['WiFi', 'Desayuno'])

    const unidades = screen.getByRole('region', { name: 'Habitaciones' })
    expect(within(unidades).getAllByRole('heading', { level: 3 }).map((h) => h.textContent)).toEqual([
      'Doble',
      'Suite familiar',
    ])
    // El precio llega como texto con centavos: se muestra exacto.
    expect(within(unidades).getByText(/260\.000,50 por noche/)).toBeInTheDocument()
    expect(within(unidades).getByText('4 huéspedes por habitación')).toBeInTheDocument()
    // La segunda imagen es una miniatura con texto alternativo.
    expect(screen.getByRole('img', { name: 'Foto 2 de Hotel Casa Tayrona' })).toBeInTheDocument()

    expect(screen.getByRole('link', { name: 'Ver disponibilidad y reservar' })).toHaveAttribute(
      'href',
      '/servicios/s-hotel/reservar',
    )
  })

  it('un tour muestra duración, punto de encuentro, dificultad y aforo', async () => {
    simularFetch({ [`${DETALLE}/s-tour-cafe`]: () => respuestaJson(200, DETALLE_TOUR) })
    renderApp('/servicios/s-tour-cafe')

    const datos = await screen.findByRole('region', { name: 'Datos del servicio' })
    expect(within(datos).getByText('4 h')).toBeInTheDocument()
    expect(within(datos).getByText('Iglesia de Minca')).toBeInTheDocument()
    expect(within(datos).getByText('Media')).toBeInTheDocument()
    expect(within(datos).getByText('12 personas')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver disponibilidad y reservar' })).toBeInTheDocument()
  })

  it('una categoría que no se reserva (Gastronomía) no tiene botón de reserva, y lo que falta no se muestra', async () => {
    simularFetch({ [`${DETALLE}/s-restaurante`]: () => respuestaJson(200, DETALLE_RESTAURANTE) })
    renderApp('/servicios/s-restaurante')

    const datos = await screen.findByRole('region', { name: 'Datos del servicio' })
    expect(within(datos).getByText('Caribeña')).toBeInTheDocument()
    expect(within(datos).getByText('Sí')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /reservar/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Descripción' })).not.toBeInTheDocument()
  })

  it('«Volver a la búsqueda» conserva los filtros de la búsqueda de origen', async () => {
    simularFetch({
      'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([HOTEL])),
      [`${DETALLE}/s-hotel`]: () => respuestaJson(200, DETALLE_HOTEL),
    })
    const { usuario } = renderApp('/explorar?texto=hotel&categoria=Alojamiento')

    const tarjeta = await screen.findByRole('article', { name: 'Hotel Casa Tayrona' })
    await usuario.click(within(tarjeta).getByRole('link', { name: 'Ver detalle' }))
    await screen.findByRole('heading', { level: 1, name: 'Hotel Casa Tayrona' })

    const volver = screen.getByRole('link', { name: /Volver a la búsqueda/ })
    expect(volver).toHaveAttribute('href', '/explorar?texto=hotel&categoria=Alojamiento')
    // El enlace a WF-10 también recuerda la búsqueda para volver desde allí.
    await usuario.click(volver)
    await waitFor(() => expect(ubicacionActual()).toBe('/explorar?texto=hotel&categoria=Alojamiento'))
  })

  it('sin búsqueda de origen, «Volver a la búsqueda» lleva a /explorar', async () => {
    simularFetch({ [`${DETALLE}/s-hotel`]: () => respuestaJson(200, DETALLE_HOTEL) })
    renderApp('/servicios/s-hotel')

    await screen.findByRole('heading', { level: 1, name: 'Hotel Casa Tayrona' })
    expect(screen.getByRole('link', { name: /Volver a la búsqueda/ })).toHaveAttribute('href', '/explorar')
  })

  it('un servicio no visible (404) lo dice, sin «Reintentar»', async () => {
    simularFetch({ [`${DETALLE}/s-oculto`]: () => respuestaProblema(404, 'OFERTA_NO_DISPONIBLE') })
    renderApp('/servicios/s-oculto')

    expect(await screen.findByRole('alert')).toHaveTextContent('Este servicio no está disponible.')
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument()
  })

  it('con el catálogo caído (503) avisa y «Reintentar» vuelve a pedir el detalle', async () => {
    let llamadas = 0
    simularFetch({
      [`${DETALLE}/s-hotel`]: () => {
        llamadas += 1
        return llamadas === 1 ? respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE') : respuestaJson(200, DETALLE_HOTEL)
      },
    })
    const { usuario } = renderApp('/servicios/s-hotel')

    expect(await screen.findByRole('alert')).toHaveTextContent('El catálogo no está disponible en este momento.')
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Hotel Casa Tayrona' })).toBeInTheDocument()
    expect(llamadas).toBe(2)
  })
})
