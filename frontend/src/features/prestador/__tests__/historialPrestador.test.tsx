import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  USUARIO_PRESTADOR,
  paginaReservasPrestador,
  paginaServicios,
  reservaPrestador,
  respuestaJson,
  respuestaProblema,
  servicioResumen,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

/** WF-23 «Historial de reservas» del Prestador (RF12, CU «Consultar historial»; DEC-196, DEC-204). */

const ME = 'GET /api/v1/auth/me'
const HISTORIAL = 'GET /api/v1/reservas/prestador'
const SERVICIOS = 'GET /api/v1/catalogo/prestador/servicios'
const TOUR = '6a3000000000000000000001'
const HOTEL = '6a3000000000000000000003'
const LENTO = { timeout: 15_000 }

type Manejador = (solicitud: SolicitudSimulada) => Response | Promise<Response>

const SERVICIOS_PRESTADOR = [
  servicioResumen({ id: TOUR }),
  servicioResumen({ id: HOTEL, nombreComercial: 'Hotel Casa Tayrona', categoria: 'Alojamiento', estadoPublicacion: 'Publicado' }),
]

const HOTEL_CANCELADA = reservaPrestador({
  idReserva: '7b1d6f0e-0000-4000-8000-000000000002',
  estado: 'Cancelada',
  idServicio: HOTEL,
  nombreServicio: 'Hotel Casa Tayrona',
  categoria: 'Alojamiento',
  idUnidad: 'u-suite',
  nombreUnidad: 'Suite familiar',
  fechaEntrada: '2026-12-20',
  fechaSalida: '2026-12-22',
  horaInicio: null,
  cantidad: 1,
  personas: 2,
  precioUnitario: '260000.50',
  unidadCobro: 'Noche',
  precioTotal: '520001.00',
  fechaCancelacion: '2026-11-05T13:00:00Z',
})

function montar(ruta: string, rutas: Record<string, Manejador> = {}) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  const simulado = simularFetch({
    [ME]: () => respuestaJson(200, USUARIO_PRESTADOR),
    [SERVICIOS]: () => respuestaJson(200, paginaServicios(SERVICIOS_PRESTADOR)),
    [HISTORIAL]: () => respuestaJson(200, paginaReservasPrestador([reservaPrestador(), HOTEL_CANCELADA])),
    ...rutas,
  })
  return { ...simulado, ...renderApp(ruta) }
}

const consultas = (solicitudes: SolicitudSimulada[]) =>
  solicitudes.filter((s) => s.url.startsWith('/api/v1/reservas/prestador?')).map((s) => s.url.split('?')[1])

describe('Historial de reservas del Prestador (RF12)', () => {
  it('el menú lleva al historial; sin filtros consulta la página 0 y muestra las reservas sin datos del Turista', async () => {
    const { solicitudes, usuario } = montar('/', { 'GET /api/v1/catalogo/oferta': () => respuestaJson(200, { elementos: [], pagina: 0, tamano: 10, totalElementos: 0, totalPaginas: 0 }) })
    await usuario.click(await screen.findByRole('button', { name: 'Menú de usuario de Casa Tayrona SAS' }))
    await usuario.click(within(screen.getByRole('menu')).getByRole('menuitem', { name: 'Historial de reservas' }))

    await screen.findByRole('heading', { level: 1, name: 'Historial de reservas' }, LENTO)
    expect(await screen.findByRole('heading', { level: 2, name: '2 reservas' }, LENTO)).toBeInTheDocument()
    expect(consultas(solicitudes)).toEqual(['pagina=0'])

    const tarjetas = screen.getAllByRole('article')
    expect(within(tarjetas[0]).getByText('Tour de Café en Minca')).toBeInTheDocument()
    expect(within(tarjetas[0]).getByText(/3 personas/)).toBeInTheDocument()
    expect(within(tarjetas[0]).getByText(/80\.000 por persona/)).toBeInTheDocument()
    expect(within(tarjetas[1]).getByText(/Suite familiar/)).toBeInTheDocument()
    expect(within(tarjetas[1]).getByText(/por noche/)).toBeInTheDocument()
    expect(within(tarjetas[1]).getByText('Cancelada el')).toBeInTheDocument()
    expect(within(tarjetas[1]).getByText(/Cancelada$/, { selector: 'p' })).toBeInTheDocument()
    // Ningún dato del Turista ni acciones de cancelar.
    expect(screen.queryByText(/turista@samaria360\.test|Ana Pérez/)).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Cancelar reserva/ })).not.toBeInTheDocument()
  })

  it('los filtros van en la URL (desde, hasta, estado múltiple y servicio) y vuelven a la página 1', async () => {
    const { solicitudes, usuario } = montar('/prestador/reservas?pagina=2')
    await screen.findByRole('heading', { level: 2, name: '2 reservas' }, LENTO)
    expect(consultas(solicitudes)).toEqual(['pagina=1'])

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-12-01' } })
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-12-31' } })
    await usuario.click(screen.getByRole('checkbox', { name: 'Confirmada' }))
    await usuario.click(screen.getByRole('checkbox', { name: 'Cancelada' }))
    const selector = screen.getByLabelText('Servicio')
    await waitFor(() => expect(selector).toBeEnabled())
    await usuario.selectOptions(selector, HOTEL)

    const esperada = `desde=2026-12-01&hasta=2026-12-31&estado=Confirmada%2CCancelada&servicio=${HOTEL}`
    await waitFor(() => expect(ubicacionActual()).toBe(`/prestador/reservas?${esperada}`))
    await waitFor(() => expect(consultas(solicitudes).at(-1)).toBe(`${esperada}&pagina=0`))
  })

  it('el selector lista los servicios propios (todas las páginas) y una página 2 va a la API como 1', async () => {
    const pagina1 = Array.from({ length: 10 }, (_v, i) => servicioResumen({ id: `6a30000000000000000001${String(i).padStart(2, '0')}`, nombreComercial: `Servicio ${i}` }))
    const { solicitudes, usuario } = montar('/prestador/reservas', {
      [SERVICIOS]: (s) =>
        respuestaJson(200, s.url.includes('pagina=0') ? paginaServicios(pagina1, 0, 11) : paginaServicios([SERVICIOS_PRESTADOR[1]], 1, 11)),
      [HISTORIAL]: () => respuestaJson(200, paginaReservasPrestador([reservaPrestador()], 0, 25)),
    })
    const selector = await screen.findByLabelText('Servicio', {}, LENTO)
    await waitFor(() => expect(within(selector).getAllByRole('option')).toHaveLength(12))
    expect(within(selector).getByRole('option', { name: 'Hotel Casa Tayrona · Alojamiento' })).toBeInTheDocument()
    expect(solicitudes.filter((s) => s.url.startsWith('/api/v1/catalogo/prestador/servicios?'))).toHaveLength(2)

    await usuario.click(await screen.findByRole('button', { name: /^(Página )?2$/ }, LENTO))
    await waitFor(() => expect(ubicacionActual()).toBe('/prestador/reservas?pagina=2'))
    await waitFor(() => expect(consultas(solicitudes).at(-1)).toBe('pagina=1'))
  })

  it('«hasta» antes de «desde» o más de 366 días: error junto al campo y sin consultar', async () => {
    const { solicitudes } = montar('/prestador/reservas?desde=2026-12-10&hasta=2026-12-01')
    expect(await screen.findByText('«Hasta» no puede ser anterior a «Desde».', {}, LENTO)).toBeInTheDocument()
    expect(consultas(solicitudes)).toEqual([])

    // 367 días contando los dos extremos: no se consulta.
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2027-12-11' } })
    expect(await screen.findByText('El rango puede ser de 366 días como máximo.', {}, LENTO)).toBeInTheDocument()
    expect(consultas(solicitudes)).toEqual([])

    // Exactamente 366 días: el límite está incluido.
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2027-12-10' } })
    await waitFor(() => expect(consultas(solicitudes)).toEqual(['desde=2026-12-10&hasta=2027-12-10&pagina=0']))
  })

  it('un `servicio` que no es un ObjectId se quita de la URL', async () => {
    const { solicitudes } = montar('/prestador/reservas?servicio=abc&estado=Completada')
    await waitFor(() => expect(ubicacionActual()).toBe('/prestador/reservas?estado=Completada'))
    await waitFor(() => expect(consultas(solicitudes).every((q) => !q.includes('servicio'))).toBe(true))
  })

  it('el selector con error no bloquea el historial (C7)', async () => {
    montar('/prestador/reservas', { [SERVICIOS]: () => respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE') })
    expect(await screen.findByRole('heading', { level: 2, name: '2 reservas' }, LENTO)).toBeInTheDocument()
    const selector = screen.getByLabelText('Servicio')
    await waitFor(() => expect(selector).toBeDisabled())
    expect(selector).toHaveAccessibleDescription(/el filtro por servicio no está disponible. El historial sigue funcionando/)
  })

  it('vacío sin filtros y vacío con filtros', async () => {
    montar('/prestador/reservas', { [HISTORIAL]: () => respuestaJson(200, paginaReservasPrestador([])) })
    expect(await screen.findByText('Aún no hay reservas de tus servicios.', {}, LENTO)).toBeInTheDocument()
  })

  it('vacío con filtros: «Quitar filtros»', async () => {
    const { usuario } = montar('/prestador/reservas?estado=Completada', { [HISTORIAL]: () => respuestaJson(200, paginaReservasPrestador([])) })
    expect(await screen.findByText('No hay reservas con esos filtros.', {}, LENTO)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Quitar filtros' }))
    await waitFor(() => expect(ubicacionActual()).toBe('/prestador/reservas'))
  })

  it('error de red: mensaje y «Reintentar»', async () => {
    let llamadas = 0
    const { usuario } = montar('/prestador/reservas', {
      [HISTORIAL]: () => {
        llamadas += 1
        if (llamadas === 1) throw new TypeError('Failed to fetch')
        return respuestaJson(200, paginaReservasPrestador([reservaPrestador()]))
      },
    })
    expect(await screen.findByText('No fue posible conectar con el servidor. Revisa tu conexión.', {}, LENTO)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByRole('heading', { level: 2, name: '1 reserva' }, LENTO)).toBeInTheDocument()
  })

  it('400 del servidor (`servicio: FORMATO`) junto al selector', async () => {
    montar(`/prestador/reservas?servicio=${TOUR}`, {
      [HISTORIAL]: () => respuestaProblema(400, 'DATOS_INVALIDOS', { campos: { servicio: 'FORMATO' } }),
    })
    const selector = await screen.findByLabelText('Servicio', {}, LENTO)
    await waitFor(() => expect(selector).toHaveAttribute('aria-invalid', 'true'))
  })
})
