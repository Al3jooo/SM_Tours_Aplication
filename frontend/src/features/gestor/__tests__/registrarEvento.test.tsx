import { fireEvent, screen, waitFor } from '@testing-library/react'
import type { UserEvent } from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  USUARIO_GESTOR,
  eventoGestion,
  paginaEventos,
  respuestaJson,
  respuestaProblema,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

/** WF-25 «Registrar evento» (CU «Registrar evento en agenda», RF25; DEC-245, DEC-248, DEC-249). */
const ME = 'GET /api/v1/auth/me'
const REGISTRO = 'POST /api/v1/catalogo/gestor/eventos'
const LISTA = 'GET /api/v1/catalogo/gestor/eventos'
const LENTO = { timeout: 15_000 }

function conGestor(rutas: Parameters<typeof simularFetch>[0] = {}) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  return simularFetch({
    [ME]: () => respuestaJson(200, USUARIO_GESTOR),
    [LISTA]: () => respuestaJson(200, paginaEventos([eventoGestion()])),
    ...rutas,
  })
}

async function llenar(usuario: UserEvent, fechas = { inicio: '2026-11-20', fin: '2026-11-22' }) {
  await usuario.type(await screen.findByLabelText('Nombre del evento', {}, LENTO), '  Fiestas del Mar ')
  fireEvent.change(screen.getByLabelText('Fecha de inicio'), { target: { value: fechas.inicio } })
  fireEvent.change(screen.getByLabelText('Fecha de fin'), { target: { value: fechas.fin } })
  await usuario.type(screen.getByLabelText('Ubicación'), 'Bahía de Santa Marta')
  await usuario.type(screen.getByLabelText('Descripción'), 'Regatas y desfile náutico.')
}

const registros = (solicitudes: SolicitudSimulada[]) =>
  solicitudes.filter((s) => s.metodo === 'POST' && s.url === '/api/v1/catalogo/gestor/eventos')

describe('WF-25 «Registrar evento»', () => {
  it('envía el JSON con los textos recortados y vuelve al panel con el anuncio de publicación', async () => {
    const { solicitudes } = conGestor({ [REGISTRO]: () => respuestaJson(201, eventoGestion()) })
    const { usuario } = renderApp('/gestor/eventos/nuevo')

    await llenar(usuario)
    expect(screen.getByLabelText('Fecha de inicio')).toHaveAttribute('type', 'date')
    expect(screen.getByLabelText('Fecha de fin')).not.toHaveAttribute('min')
    await usuario.click(screen.getByRole('button', { name: 'Registrar evento' }))

    await waitFor(() => expect(ubicacionActual()).toBe('/gestor/eventos'))
    expect(await screen.findByText('Evento registrado: ya está publicado en la agenda.', {}, LENTO)).toBeInTheDocument()
    const [envio] = registros(solicitudes)
    expect(envio.cuerpo).toEqual({
      nombre: 'Fiestas del Mar',
      fechaInicio: '2026-11-20',
      fechaFin: '2026-11-22',
      ubicacion: 'Bahía de Santa Marta',
      descripcion: 'Regatas y desfile náutico.',
    })
    expect(envio.cabeceras.Authorization).toBe('Bearer token-guardado')
  })

  it('vacío no envía nada: marca los cinco campos y lleva el foco al nombre', async () => {
    const { solicitudes } = conGestor()
    const { usuario } = renderApp('/gestor/eventos/nuevo')

    await usuario.click(await screen.findByRole('button', { name: 'Registrar evento' }, LENTO))

    expect(registros(solicitudes)).toHaveLength(0)
    for (const etiqueta of ['Nombre del evento', 'Fecha de inicio', 'Fecha de fin', 'Ubicación', 'Descripción']) {
      expect(screen.getByLabelText(etiqueta)).toHaveAttribute('aria-invalid', 'true')
    }
    await waitFor(() => expect(screen.getByLabelText('Nombre del evento')).toHaveFocus())
  })

  it('un fin anterior al inicio se frena en el cliente, sin POST (DEC-237)', async () => {
    const { solicitudes } = conGestor()
    const { usuario } = renderApp('/gestor/eventos/nuevo')

    await llenar(usuario, { inicio: '2026-11-22', fin: '2026-11-20' })
    await usuario.click(screen.getByRole('button', { name: 'Registrar evento' }))

    expect(await screen.findByText('La fecha de fin no puede ser anterior a la de inicio.')).toBeInTheDocument()
    expect(registros(solicitudes)).toHaveLength(0)
    await waitFor(() => expect(screen.getByLabelText('Fecha de fin')).toHaveFocus())
  })

  it('un fin que ya pasó lo decide el servidor: 400 NO_PERMITIDO en el fin, sin perder lo escrito (DEC-242)', async () => {
    const { solicitudes } = conGestor({
      [REGISTRO]: () => respuestaProblema(400, 'DATOS_INVALIDOS', { campos: { fechaFin: 'NO_PERMITIDO' } }),
    })
    const { usuario } = renderApp('/gestor/eventos/nuevo')

    await llenar(usuario, { inicio: '2000-01-01', fin: '2000-01-02' })
    await usuario.click(screen.getByRole('button', { name: 'Registrar evento' }))

    expect(
      await screen.findByText('La fecha de fin ya pasó (hoy en Santa Marta). Un evento terminado no aparecería en la agenda.'),
    ).toBeInTheDocument()
    expect(registros(solicitudes)).toHaveLength(1)
    expect(ubicacionActual()).toBe('/gestor/eventos/nuevo')
    expect(screen.getByLabelText('Nombre del evento')).toHaveValue('  Fiestas del Mar ')
    await waitFor(() => expect(screen.getByLabelText('Fecha de fin')).toHaveFocus())
    // Cambiar una fecha quita el error.
    fireEvent.change(screen.getByLabelText('Fecha de fin'), { target: { value: '2026-11-22' } })
    expect(screen.getByLabelText('Fecha de fin')).not.toHaveAttribute('aria-invalid')
  })

  it('un 400 con un campo sin control es un mensaje general', async () => {
    conGestor({ [REGISTRO]: () => respuestaProblema(400, 'DATOS_INVALIDOS', { campos: { gestorId: 'NO_PERMITIDO' } }) })
    const { usuario } = renderApp('/gestor/eventos/nuevo')

    await llenar(usuario)
    await usuario.click(screen.getByRole('button', { name: 'Registrar evento' }))

    expect(
      await screen.findByText('Revisa los datos: hay errores que no corresponden a un campo de esta pantalla.'),
    ).toHaveFocus()
  })

  it.each([
    ['403 ACCESO_DENEGADO', () => respuestaProblema(403, 'ACCESO_DENEGADO'), 'Tu cuenta no puede registrar eventos.'],
    [
      '503',
      () => respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE', { servicio: 'ms-catalog' }),
      'El catálogo no responde en este momento. Tus datos siguen aquí: inténtalo de nuevo.',
    ],
    [
      'un error de red',
      () => {
        throw new TypeError('Failed to fetch')
      },
      'No hay conexión. Tus datos siguen aquí: inténtalo de nuevo.',
    ],
  ])('%s: mensaje general con el foco y lo escrito se conserva', async (_caso, respuesta, mensaje) => {
    conGestor({ [REGISTRO]: respuesta })
    const { usuario } = renderApp('/gestor/eventos/nuevo')

    await llenar(usuario)
    await usuario.click(screen.getByRole('button', { name: 'Registrar evento' }))

    expect(await screen.findByText(mensaje)).toHaveFocus()
    expect(screen.getByLabelText('Ubicación')).toHaveValue('Bahía de Santa Marta')
  })

  it('un solo envío a la vez', async () => {
    let liberar: (respuesta: Response) => void = () => {}
    const { solicitudes } = conGestor({ [REGISTRO]: () => new Promise<Response>((resolver) => (liberar = resolver)) })
    const { usuario } = renderApp('/gestor/eventos/nuevo')

    await llenar(usuario)
    await usuario.click(screen.getByRole('button', { name: 'Registrar evento' }))
    const boton = await screen.findByRole('button', { name: 'Registrando…' })
    expect(boton).toBeDisabled()
    await usuario.click(boton)
    expect(registros(solicitudes)).toHaveLength(1)

    liberar(respuestaJson(201, eventoGestion()))
    await waitFor(() => expect(ubicacionActual()).toBe('/gestor/eventos'))
  })

  it('los contadores cuentan puntos de código: 120, 200 y 1000', async () => {
    conGestor()
    const { usuario } = renderApp('/gestor/eventos/nuevo')

    await usuario.type(await screen.findByLabelText('Nombre del evento', {}, LENTO), '🎉🎉')
    expect(screen.getByText('2/120')).toBeInTheDocument()
    expect(screen.getByText('0/200')).toBeInTheDocument()
    expect(screen.getByText('0/1000')).toBeInTheDocument()
  })
})
