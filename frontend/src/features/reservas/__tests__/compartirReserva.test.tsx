import { screen, waitFor, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  USUARIO_TURISTA,
  paginaReservas,
  reservaConfirmada,
  respuestaJson,
  simularFetch,
} from '../../../test/fetchSimulado'
import { renderApp } from '../../../test/renderApp'

const CONFIRMADA = reservaConfirmada({ idReserva: 'r-confirmada', cancelableHasta: new Date(Date.now() + 72 * 3_600_000).toISOString() })
const CANCELADA = reservaConfirmada({ idReserva: 'r-cancelada', estado: 'Cancelada', nombreServicio: 'Hotel Cancelado' })
const COMPLETADA = reservaConfirmada({
  idReserva: 'r-completada',
  estado: 'Completada',
  idServicio: 's-kayak',
  nombreServicio: 'Kayak en Taganga',
})

const ENLACE = () => `${window.location.origin}/servicios/s-kayak`

/** La primera carga de una página diferida puede pasar de 5 s con toda la suite en paralelo (ver test/setup.ts). */
const LENTO = { timeout: 15_000 }

/** Reemplaza (o quita, con `undefined`) una capacidad de navigator; se restaura después de cada prueba. */
const originales = new Map<string, PropertyDescriptor | undefined>()
function simularNavegador(propiedad: 'share' | 'clipboard', valor: unknown) {
  if (!originales.has(propiedad)) originales.set(propiedad, Object.getOwnPropertyDescriptor(navigator, propiedad))
  Object.defineProperty(navigator, propiedad, { configurable: true, value: valor })
}

afterEach(() => {
  for (const [propiedad, descriptor] of originales) {
    if (descriptor) Object.defineProperty(navigator, propiedad, descriptor)
    else delete (navigator as unknown as Record<string, unknown>)[propiedad]
  }
  originales.clear()
})

async function abrirMisReservas() {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  simularFetch({
    'GET /api/v1/auth/me': () => respuestaJson(200, USUARIO_TURISTA),
    'GET /api/v1/reservas/mias': () => respuestaJson(200, paginaReservas([CONFIRMADA, CANCELADA, COMPLETADA])),
  })
  const app = renderApp('/mis-reservas')
  await screen.findByRole('heading', { level: 2, name: '3 reservas' }, LENTO)
  return app
}

const botonCompartir = () => screen.getByRole('button', { name: 'Compartir Kayak en Taganga' })

describe('«Compartir» en «Mis reservas» (RF49, DEC-81, DEC-100)', () => {
  it('solo las reservas Completadas tienen «Compartir»', async () => {
    await abrirMisReservas()

    expect(screen.getAllByRole('button', { name: /^Compartir/ })).toHaveLength(1)
    expect(within(screen.getByRole('article', { name: 'Kayak en Taganga' })).getByRole('button', { name: /^Compartir/ })).toBe(
      botonCompartir(),
    )
    expect(botonCompartir()).toHaveTextContent('Compartir')
  })

  it('con la Web Share API comparte la URL absoluta del servicio con su nombre', async () => {
    const { usuario } = await abrirMisReservas()
    const share = vi.fn(async () => {})
    simularNavegador('share', share)

    await usuario.click(botonCompartir())

    await waitFor(() => expect(share).toHaveBeenCalledWith({ title: 'Kayak en Taganga', url: ENLACE() }))
    expect(screen.queryByText('Enlace copiado.')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Enlace para compartir')).not.toBeInTheDocument()
  })

  it('sin Web Share API copia el enlace al portapapeles y anuncia «Enlace copiado»', async () => {
    const { usuario } = await abrirMisReservas()
    const writeText = vi.fn(async () => {})
    simularNavegador('share', undefined)
    simularNavegador('clipboard', { writeText })

    await usuario.click(botonCompartir())

    expect(await screen.findByText('Enlace copiado.')).toHaveAttribute('role', 'status')
    expect(writeText).toHaveBeenCalledWith(ENLACE())
  })

  it('si la hoja de compartir falla por otro motivo, también copia al portapapeles', async () => {
    const { usuario } = await abrirMisReservas()
    const writeText = vi.fn(async () => {})
    simularNavegador('share', vi.fn(async () => Promise.reject(new DOMException('', 'NotAllowedError'))))
    simularNavegador('clipboard', { writeText })

    await usuario.click(botonCompartir())

    expect(await screen.findByText('Enlace copiado.')).toBeInTheDocument()
    expect(writeText).toHaveBeenCalledWith(ENLACE())
  })

  it('si tampoco se puede copiar, muestra el enlace en un campo de solo lectura, seleccionado', async () => {
    const { usuario } = await abrirMisReservas()
    simularNavegador('share', undefined)
    simularNavegador('clipboard', { writeText: vi.fn(async () => Promise.reject(new DOMException('', 'NotAllowedError'))) })

    await usuario.click(botonCompartir())

    const campo = await screen.findByLabelText('Enlace para compartir')
    expect(campo).toHaveValue(ENLACE())
    expect(campo).toHaveAttribute('readonly')
    await waitFor(() => expect(campo).toHaveFocus())
    expect(campo).toHaveAccessibleDescription('No pudimos copiarlo automáticamente: cópialo desde este campo.')
    expect(screen.queryByText('Enlace copiado.')).not.toBeInTheDocument()
  })

  it('cancelar la hoja de compartir (AbortError) no es un error ni copia nada', async () => {
    const { usuario } = await abrirMisReservas()
    const writeText = vi.fn(async () => {})
    const share = vi.fn(async () => Promise.reject(new DOMException('', 'AbortError')))
    simularNavegador('share', share)
    simularNavegador('clipboard', { writeText })

    await usuario.click(botonCompartir())

    await waitFor(() => expect(share).toHaveBeenCalled())
    expect(writeText).not.toHaveBeenCalled()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(screen.queryByText('Enlace copiado.')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Enlace para compartir')).not.toBeInTheDocument()
  })
})
