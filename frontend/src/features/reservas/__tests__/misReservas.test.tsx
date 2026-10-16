import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import {
  USUARIO_PRESTADOR,
  USUARIO_TURISTA,
  paginaReservas,
  reservaConfirmada,
  respuestaJson,
  respuestaProblema,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

const MIAS = 'GET /api/v1/reservas/mias'
const ME = 'GET /api/v1/auth/me'
const HORA = 3_600_000

/** Una reserva cuyo plazo de cancelación (lo calcula ms-booking) termina dentro de `horas` (negativo: ya pasó). */
const conPlazo = (horas: number) => new Date(Date.now() + horas * HORA).toISOString()

const FUTURA = reservaConfirmada({ idReserva: 'r-futura', cancelableHasta: conPlazo(72) })
const CERCANA = reservaConfirmada({
  idReserva: 'r-cercana',
  nombreServicio: 'Tour de Café en Minca',
  categoria: 'Tour',
  idUnidad: null,
  nombreUnidad: null,
  fechaSalida: null,
  horaInicio: '08:00:00',
  cantidad: 2,
  personas: 2,
  precioTotal: 160000,
  cancelableHasta: conPlazo(-2),
})
const CANCELADA = reservaConfirmada({ idReserva: 'r-cancelada', estado: 'Cancelada', fechaCancelacion: conPlazo(-24) })
const COMPLETADA = reservaConfirmada({ idReserva: 'r-completada', estado: 'Completada', nombreServicio: 'Kayak en Taganga' })

const consultas = (solicitudes: SolicitudSimulada[]) =>
  solicitudes.filter((s) => s.url.startsWith('/api/v1/reservas/mias')).map((s) => s.url.split('?')[1] ?? '')

function comoTurista(rutas: Parameters<typeof simularFetch>[0] = {}, usuario: object = USUARIO_TURISTA) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  return simularFetch({ [ME]: () => respuestaJson(200, usuario), ...rutas })
}

const tarjeta = (nombre: string) => screen.getByRole('article', { name: nombre })

describe('WF-11 «Mis reservas»: acceso', () => {
  it('sin sesión lleva al login', async () => {
    simularFetch({})
    renderApp('/mis-reservas')
    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
  })

  it('con otro rol, un aviso y ninguna consulta', async () => {
    const { solicitudes } = comoTurista({}, USUARIO_PRESTADOR)
    renderApp('/mis-reservas')

    expect(await screen.findByRole('alert')).toHaveTextContent('«Mis reservas» es solo para cuentas de turista.')
    expect(consultas(solicitudes)).toHaveLength(0)
  })
})

describe('WF-11 «Mis reservas»: lista y filtros (RF12)', () => {
  it('lista las reservas con su estado en texto e icono y anuncia cuántas hay', async () => {
    comoTurista({ [MIAS]: () => respuestaJson(200, paginaReservas([FUTURA, CERCANA, CANCELADA, COMPLETADA])) })
    renderApp('/mis-reservas')

    expect(await screen.findByRole('heading', { level: 2, name: '4 reservas' })).toBeInTheDocument()
    const lista = screen.getAllByRole('article')
    expect(lista).toHaveLength(4)
    expect(within(lista[0]).getByText('Confirmada')).toHaveTextContent('Estado: Confirmada')
    expect(within(lista[0]).getByText('Suite familiar')).toBeInTheDocument()
    expect(within(lista[0]).getByText(/3 noches/)).toBeInTheDocument()
    expect(within(lista[0]).getByText('2 habitaciones · 5 huéspedes')).toBeInTheDocument()
    expect(within(lista[1]).getByText(/· 08:00$/)).toBeInTheDocument()
    expect(within(lista[2]).getByText('Cancelada')).toBeInTheDocument()
    expect(within(lista[2]).queryByRole('button')).not.toBeInTheDocument()
    // Las Completadas no se cancelan.
    expect(within(tarjeta('Kayak en Taganga')).getByText('Completada')).toBeInTheDocument()
    expect(within(tarjeta('Kayak en Taganga')).queryByRole('button')).not.toBeInTheDocument()
  })

  it('los filtros van en la URL, vuelven a la página 1 y llegan a la API', async () => {
    const { solicitudes } = comoTurista({ [MIAS]: () => respuestaJson(200, paginaReservas([FUTURA])) })
    const { usuario } = renderApp('/mis-reservas?pagina=2')

    await screen.findByRole('heading', { level: 2, name: '1 reserva' })
    expect(consultas(solicitudes).at(-1)).toBe('pagina=1')

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-12-01' } })
    await waitFor(() => expect(ubicacionActual()).toBe('/mis-reservas?desde=2026-12-01'))
    await usuario.click(screen.getByRole('checkbox', { name: 'Cancelada' }))
    await usuario.click(screen.getByRole('checkbox', { name: 'Confirmada' }))
    await waitFor(() => expect(ubicacionActual()).toBe('/mis-reservas?desde=2026-12-01&estado=Confirmada%2CCancelada'))
    await waitFor(() => expect(consultas(solicitudes).at(-1)).toBe('desde=2026-12-01&estado=Confirmada%2CCancelada&pagina=0'))

    await usuario.click(screen.getByRole('button', { name: 'Quitar filtros' }))
    await waitFor(() => expect(ubicacionActual()).toBe('/mis-reservas'))
  })

  it('«Hasta» anterior a «Desde» muestra el error y no consulta', async () => {
    const { solicitudes } = comoTurista({ [MIAS]: () => respuestaJson(200, paginaReservas([])) })
    renderApp('/mis-reservas?desde=2026-12-31&hasta=2026-12-01')

    const hasta = await screen.findByLabelText('Hasta')
    expect(hasta).toHaveAccessibleDescription('«Hasta» no puede ser anterior a «Desde».')
    expect(hasta).toHaveAttribute('aria-invalid', 'true')
    expect(consultas(solicitudes)).toHaveLength(0)
  })

  it('un DATOS_INVALIDOS de la API se muestra en su campo', async () => {
    comoTurista({ [MIAS]: () => respuestaProblema(400, 'DATOS_INVALIDOS', { campos: { hasta: 'MAXIMO_DIAS' } }) })
    renderApp('/mis-reservas?desde=2026-01-01&hasta=2026-12-31')

    await waitFor(() =>
      expect(screen.getByLabelText('Hasta')).toHaveAccessibleDescription('El rango puede ser de 366 días como máximo.'),
    )
  })

  it.each([
    ['sin filtros', '/mis-reservas', 'Aún no tienes reservas.'],
    ['con filtros', '/mis-reservas?estado=Completada', 'No hay reservas con estos filtros.'],
  ])('vacío %s: «%s»', async (_caso, ruta, mensaje) => {
    comoTurista({ [MIAS]: () => respuestaJson(200, paginaReservas([])) })
    renderApp(ruta)

    expect(await screen.findByText(mensaje)).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: '0 reservas' })).toBeInTheDocument()
  })
})

describe('WF-11 «Mis reservas»: cancelar (RF18, RF44)', () => {
  it('a menos de 24 horas, «Cancelar» queda con aria-disabled y el motivo vinculado', async () => {
    comoTurista({ [MIAS]: () => respuestaJson(200, paginaReservas([CERCANA])) })
    renderApp('/mis-reservas')

    const boton = within(await screen.findByRole('article', { name: 'Tour de Café en Minca' })).getByRole('button', {
      name: 'Cancelar reserva',
    })
    expect(boton).toHaveAttribute('aria-disabled', 'true')
    expect(boton).toHaveAccessibleDescription(expect.stringContaining('Ya no se puede cancelar: el plazo terminó'))
  })

  it('el diálogo nativo se abre con showModal(); Esc y «No, volver» lo cierran y devuelven el foco', async () => {
    const showModal = vi.spyOn(HTMLDialogElement.prototype, 'showModal')
    comoTurista({ [MIAS]: () => respuestaJson(200, paginaReservas([FUTURA])) })
    const { usuario } = renderApp('/mis-reservas')

    const boton = within(await screen.findByRole('article', { name: 'Hotel Casa Tayrona' })).getByRole('button', {
      name: 'Cancelar reserva',
    })
    expect(boton).toHaveAccessibleDescription(expect.stringContaining('Cancelación gratuita hasta el'))
    await usuario.click(boton)

    const dialogo = screen.getByRole('dialog', { name: '¿Cancelar esta reserva?' })
    expect(showModal).toHaveBeenCalledTimes(1)
    expect(dialogo).toHaveAccessibleDescription(expect.stringContaining('Hotel Casa Tayrona'))
    expect(within(dialogo).getByRole('button', { name: 'No, volver' })).toHaveFocus()

    await usuario.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(boton).toHaveFocus()

    await usuario.click(boton)
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'No, volver' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(boton).toHaveFocus()
  })

  it('al cancelar, la tarjeta pasa a Cancelada, se anuncia y el foco va a su título', async () => {
    const { solicitudes } = comoTurista({
      [MIAS]: () => respuestaJson(200, paginaReservas([FUTURA])),
      'POST /api/v1/reservas/r-futura/cancelacion': () =>
        respuestaJson(200, { ...FUTURA, estado: 'Cancelada', fechaCancelacion: new Date().toISOString() }),
    })
    const { usuario } = renderApp('/mis-reservas')

    const reserva = await screen.findByRole('article', { name: 'Hotel Casa Tayrona' })
    await usuario.click(within(reserva).getByRole('button', { name: 'Cancelar reserva' }))
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Sí, cancelar' }))

    expect(await screen.findByText('Cancelaste la reserva de Hotel Casa Tayrona. Los cupos quedaron libres.')).toHaveAttribute(
      'role',
      'status',
    )
    const actualizada = screen.getByRole('article', { name: 'Hotel Casa Tayrona' })
    expect(within(actualizada).getByText('Cancelada')).toBeInTheDocument()
    expect(within(actualizada).queryByRole('button')).not.toBeInTheDocument()
    expect(within(actualizada).getByRole('heading', { level: 3 })).toHaveFocus()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(solicitudes.filter((s) => s.metodo === 'POST')).toHaveLength(1)
  })

  it('el 409 de RF44 se muestra en el diálogo y ya no ofrece cancelar', async () => {
    comoTurista({
      [MIAS]: () => respuestaJson(200, paginaReservas([FUTURA])),
      'POST /api/v1/reservas/r-futura/cancelacion': () => respuestaProblema(409, 'CANCELACION_FUERA_DE_PLAZO'),
    })
    const { usuario } = renderApp('/mis-reservas')

    await usuario.click(
      within(await screen.findByRole('article', { name: 'Hotel Casa Tayrona' })).getByRole('button', { name: 'Cancelar reserva' }),
    )
    const dialogo = screen.getByRole('dialog')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Sí, cancelar' }))

    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('faltan menos de 24 horas')
    expect(within(dialogo).queryByRole('button', { name: 'Sí, cancelar' })).not.toBeInTheDocument()
    expect(within(dialogo).getByRole('button', { name: 'Cerrar' })).toBeInTheDocument()
  })

  it('si RF52 la completó mientras tanto (RESERVA_NO_CANCELABLE), cierra el diálogo, avisa y vuelve a consultar', async () => {
    let consultasHechas = 0
    comoTurista({
      [MIAS]: () => {
        consultasHechas += 1
        return respuestaJson(200, paginaReservas([consultasHechas === 1 ? FUTURA : { ...FUTURA, estado: 'Completada' }]))
      },
      'POST /api/v1/reservas/r-futura/cancelacion': () => respuestaProblema(409, 'RESERVA_NO_CANCELABLE'),
    })
    const { usuario } = renderApp('/mis-reservas')

    await usuario.click(
      within(await screen.findByRole('article', { name: 'Hotel Casa Tayrona' })).getByRole('button', { name: 'Cancelar reserva' }),
    )
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Sí, cancelar' }))

    expect(await screen.findByText(/ya se completó y no se puede cancelar/)).toHaveAttribute('role', 'alert')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await waitFor(() => expect(within(tarjeta('Hotel Casa Tayrona')).getByText('Completada')).toBeInTheDocument())
    expect(consultasHechas).toBe(2)
  })
})
