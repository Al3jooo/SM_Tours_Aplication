import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  CASCADA_CERRADA,
  HOTEL,
  TOUR_CAFE,
  paginaOferta,
  respuestaHtml,
  respuestaJson,
  respuestaProblema,
  simularFetch,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

const DESTACADOS = 'GET /api/v1/catalogo/oferta?orden=PrecioAscendente&pagina=0&tamano=3'

describe('Inicio WF-04', () => {
  // Límite de 15 s: medida en más de 2 s (primera del archivo: primer render de la app y 15 pulsaciones de usuario.type).
  it('la búsqueda lleva a /explorar con el texto', async () => {
    simularFetch({ [DESTACADOS]: () => respuestaJson(200, paginaOferta([])) })
    const { usuario } = renderApp('/')

    await usuario.type(screen.getByRole('searchbox', { name: '¿Qué buscas?' }), '  tour de café ')
    await usuario.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(ubicacionActual()).toBe('/explorar?texto=tour+de+caf%C3%A9')
  }, 15_000)

  it('la búsqueda vacía lleva a /explorar sin filtros', async () => {
    simularFetch({ [DESTACADOS]: () => respuestaJson(200, paginaOferta([])) })
    const { usuario } = renderApp('/')

    await usuario.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(ubicacionActual()).toBe('/explorar')
  })

  it.each([
    ['Alojamiento', 'Alojamiento'],
    ['Gastronomía', 'Gastronomia'],
    ['Tours', 'Tour'],
    ['Alquiler de equipos', 'AlquilerEquipo'],
  ])('el botón de categoría «%s» lleva a /explorar?categoria=%s', async (etiqueta, categoria) => {
    simularFetch({ 'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([])) })
    const { usuario } = renderApp('/')

    const categorias = screen.getByRole('region', { name: 'Explora por categoría' })
    await usuario.click(within(categorias).getByRole('link', { name: etiqueta }))

    expect(ubicacionActual()).toBe(`/explorar?categoria=${categoria}`)
  })

  it('los destacados piden 3 por precio ascendente y solo muestran servicios', async () => {
    const { solicitudes } = simularFetch({
      [DESTACADOS]: () => respuestaJson(200, paginaOferta([TOUR_CAFE, HOTEL, CASCADA_CERRADA], { tamano: 3 })),
    })
    renderApp('/')

    const destacados = screen.getByRole('region', { name: 'Destacados esta semana' })
    expect(await within(destacados).findByRole('heading', { name: 'Tour de Café en Minca' })).toBeInTheDocument()
    expect(within(destacados).getByRole('heading', { name: 'Hotel Casa Tayrona' })).toBeInTheDocument()
    // Por seguridad, el componente descarta lo que no sea servicio.
    expect(within(destacados).queryByRole('heading', { name: 'Cascada El Salto' })).not.toBeInTheDocument()
    expect(solicitudes.some((s) => s.url === DESTACADOS.replace('GET ', ''))).toBe(true)
  })

  it('si el catálogo no está disponible, solo los destacados lo indican y la búsqueda sigue funcionando', async () => {
    simularFetch({ [DESTACADOS]: () => respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE') })
    const { usuario } = renderApp('/')

    const destacados = screen.getByRole('region', { name: 'Destacados esta semana' })
    expect(await within(destacados).findByText('El catálogo no está disponible en este momento.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Encuentra alojamiento')

    await usuario.type(screen.getByRole('searchbox', { name: '¿Qué buscas?' }), 'kayak')
    await usuario.click(screen.getByRole('button', { name: 'Buscar' }))
    expect(ubicacionActual()).toBe('/explorar?texto=kayak')
  })

  it('«Búsqueda» del encabezado lleva a la exploración', async () => {
    simularFetch({ 'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([])) })
    const { usuario } = renderApp('/')

    await usuario.click(screen.getByRole('link', { name: 'Búsqueda' }))

    expect(ubicacionActual()).toBe('/explorar')
    expect(await screen.findByRole('heading', { level: 1, name: /Exploración de Atractivos/ })).toBeInTheDocument()
  })

  // 502 y 504: los genera nginx (HTML) cuando el gateway no responde; 503: el ProblemDetail del gateway o de nginx.
  it.each([
    ['502 de nginx', () => respuestaHtml(502)],
    ['503 SERVICIO_NO_DISPONIBLE', () => respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE', { servicio: 'api-gateway' })],
    ['504 de nginx', () => respuestaHtml(504)],
  ])('un %s muestra «no disponible» con «Reintentar»', async (_caso, respuesta) => {
    simularFetch({ [DESTACADOS]: respuesta })
    renderApp('/')

    const destacados = screen.getByRole('region', { name: 'Destacados esta semana' })
    expect(await within(destacados).findByText('El catálogo no está disponible en este momento.')).toBeInTheDocument()
    expect(within(destacados).getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })

  it('un 500 sigue siendo un error inesperado (defecto del servidor, no una caída)', async () => {
    simularFetch({ [DESTACADOS]: () => respuestaProblema(500, 'ERROR_INTERNO') })
    renderApp('/')

    const destacados = screen.getByRole('region', { name: 'Destacados esta semana' })
    expect(await within(destacados).findByText('Ocurrió un error inesperado al buscar.')).toBeInTheDocument()
    expect(within(destacados).queryByText('El catálogo no está disponible en este momento.')).not.toBeInTheDocument()
  })
})
