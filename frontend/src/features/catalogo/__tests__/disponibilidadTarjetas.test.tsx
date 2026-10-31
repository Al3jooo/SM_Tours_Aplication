import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  CASCADA_CERRADA,
  HOTEL,
  TOUR_CAFE,
  paginaOferta,
  respuestaJson,
  respuestaProblema,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp } from '../../../test/renderApp'
import { hoyBogota, sumarDias } from '../../reservas/fechasBogota'

const OFERTA = 'GET /api/v1/catalogo/oferta'
const RESUMEN = 'GET /api/v1/reservas/disponibilidad/resumen'
const RESTAURANTE = { ...HOTEL, id: 's-restaurante', nombre: 'Mirador Gourmet', categoria: 'Gastronomia' }

const consultasResumen = (solicitudes: SolicitudSimulada[]) =>
  solicitudes.filter((s) => s.url.startsWith('/api/v1/reservas/disponibilidad/resumen'))

/** El dato «Disponibilidad» (o «Afluencia») de una tarjeta. */
function disponibilidadDe(nombre: string) {
  const tarjeta = screen.getByRole('article', { name: nombre })
  return within(tarjeta).getByText(/^(Disponibilidad|Afluencia)$/).nextElementSibling as HTMLElement
}

describe('Disponibilidad de las tarjetas (RF11): resumen de ms-booking para los próximos 7 días', () => {
  it('una sola consulta por página, solo con los servicios que se reservan, de hoy a hoy + 6', async () => {
    const { solicitudes } = simularFetch({
      [OFERTA]: () => respuestaJson(200, paginaOferta([HOTEL, TOUR_CAFE, CASCADA_CERRADA, RESTAURANTE])),
      [RESUMEN]: () => respuestaJson(200, { 's-hotel': 'CON_CUPOS', 's-tour-cafe': 'AGOTADO' }),
    })
    renderApp('/explorar')

    await waitFor(() => expect(disponibilidadDe('Hotel Casa Tayrona')).toHaveTextContent('Disponible esta semana'))
    // El mismo texto es el que oye el lector de pantalla: sin texto oculto aparte.
    expect(disponibilidadDe('Hotel Casa Tayrona').textContent).toBe('Disponible esta semana')
    expect(disponibilidadDe('Tour de Café en Minca').textContent).toBe('Agotado los próximos 7 días')
    // Gastronomía no se reserva y los atractivos muestran afluencia: siguen con «—».
    expect(disponibilidadDe('Mirador Gourmet')).toHaveTextContent('—sin datos')
    expect(disponibilidadDe('Cascada El Salto')).toHaveTextContent('—sin datos')

    const consultas = consultasResumen(solicitudes)
    expect(consultas).toHaveLength(1)
    const hoy = hoyBogota()
    const parametros = new URL(consultas[0].url, 'http://localhost').searchParams
    expect(parametros.get('servicios')).toBe('s-hotel,s-tour-cafe')
    expect(parametros.get('desde')).toBe(hoy)
    expect(parametros.get('hasta')).toBe(sumarDias(hoy, 6))
  })

  it('«Sin fechas» cuando el servicio no tiene inventario publicado', async () => {
    simularFetch({
      [OFERTA]: () => respuestaJson(200, paginaOferta([HOTEL])),
      [RESUMEN]: () => respuestaJson(200, { 's-hotel': 'SIN_INVENTARIO' }),
    })
    renderApp('/explorar')

    await waitFor(() => expect(disponibilidadDe('Hotel Casa Tayrona').textContent).toBe('Sin fechas'))
  })

  it('si ms-booking no responde, las tarjetas siguen con «—»', async () => {
    const { solicitudes } = simularFetch({
      [OFERTA]: () => respuestaJson(200, paginaOferta([HOTEL])),
      [RESUMEN]: () => respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE', { servicio: 'ms-booking' }),
    })
    renderApp('/explorar')

    await screen.findByRole('article', { name: 'Hotel Casa Tayrona' })
    await waitFor(() => expect(consultasResumen(solicitudes)).toHaveLength(1))
    expect(disponibilidadDe('Hotel Casa Tayrona')).toHaveTextContent('—sin datos')
  })

  it('una página solo con atractivos no consulta el resumen', async () => {
    const { solicitudes } = simularFetch({ [OFERTA]: () => respuestaJson(200, paginaOferta([CASCADA_CERRADA])) })
    renderApp('/explorar')

    await screen.findByRole('article', { name: 'Cascada El Salto' })
    expect(consultasResumen(solicitudes)).toHaveLength(0)
  })

  it('en los destacados del inicio, también una sola consulta', async () => {
    const { solicitudes } = simularFetch({
      [OFERTA]: () => respuestaJson(200, paginaOferta([TOUR_CAFE, HOTEL], { tamano: 3 })),
      [RESUMEN]: () => respuestaJson(200, { 's-tour-cafe': 'CON_CUPOS', 's-hotel': 'AGOTADO' }),
    })
    renderApp('/')

    await waitFor(() => expect(disponibilidadDe('Hotel Casa Tayrona').textContent).toBe('Agotado los próximos 7 días'))
    expect(disponibilidadDe('Tour de Café en Minca').textContent).toBe('Disponible esta semana')
    expect(consultasResumen(solicitudes)).toHaveLength(1)
  })

  it('«Ver detalle» es un enlace a la ficha de su tipo: servicio o atractivo (DEC-224)', async () => {
    simularFetch({
      [OFERTA]: () => respuestaJson(200, paginaOferta([HOTEL, CASCADA_CERRADA])),
      [RESUMEN]: () => respuestaJson(200, { 's-hotel': 'CON_CUPOS' }),
    })
    renderApp('/explorar')

    const hotel = await screen.findByRole('article', { name: 'Hotel Casa Tayrona' })
    expect(within(hotel).getByRole('link', { name: 'Ver detalle' })).toHaveAttribute('href', '/servicios/s-hotel')
    const cascada = screen.getByRole('article', { name: 'Cascada El Salto' })
    expect(within(cascada).getByRole('link', { name: 'Ver detalle' })).toHaveAttribute('href', '/atractivos/a-cascada')
  })
})

describe('Tarjetas y O14 (DEC-171)', () => {
  it('un 503 del resumen con servicio ms-catalog deja «—» y no menciona el sistema de reservas', async () => {
    const { solicitudes } = simularFetch({
      [OFERTA]: () => respuestaJson(200, paginaOferta([HOTEL])),
      [RESUMEN]: () => respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE', { servicio: 'ms-catalog' }),
    })
    renderApp('/explorar')

    await screen.findByRole('article', { name: 'Hotel Casa Tayrona' })
    await waitFor(() => expect(consultasResumen(solicitudes)).toHaveLength(1))
    expect(disponibilidadDe('Hotel Casa Tayrona')).toHaveTextContent('—sin datos')
    expect(screen.queryByText(/sistema de reservas/)).not.toBeInTheDocument()
  })
})
