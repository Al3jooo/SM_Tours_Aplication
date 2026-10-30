import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  USUARIO_PRESTADOR,
  respuestaJson,
  respuestaProblema,
  servicioGestion,
  servicioGestionHotel,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

/** WF-20 «Actualizar precio» (RF32, CU «Actualizar tarifas»; DEC-196, DEC-198, DEC-200, DEC-201). */

const ME = 'GET /api/v1/auth/me'
const ID = '6a3000000000000000000001'
const ID_HOTEL = '6a3000000000000000000003'
const BASE = '/api/v1/catalogo/prestador/servicios'
const LENTO = { timeout: 15_000 }

type Manejador = (solicitud: SolicitudSimulada) => Response | Promise<Response>

function montar(ruta: string, rutas: Record<string, Manejador>) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  const simulado = simularFetch({ [ME]: () => respuestaJson(200, USUARIO_PRESTADOR), ...rutas })
  return { ...simulado, ...renderApp(ruta) }
}

/** Responde en orden; la última respuesta se repite. */
function enSecuencia(...respuestas: (() => Response)[]): Manejador {
  let llamada = 0
  return () => respuestas[Math.min(llamada++, respuestas.length - 1)]()
}

const puts = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.metodo === 'PUT')
const titulo = (texto: string) => screen.findByRole('heading', { level: 1, name: texto }, LENTO)
const guardar = () => screen.getByRole('button', { name: 'Guardar cambios' })

describe('Precio de un Tour (RF32)', () => {
  it('el detalle Pendiente lleva a «Actualizar precio»; el cuerpo es `{precioBase, version}` y el 200 vuelve con el aviso', async () => {
    const actualizado = servicioGestion({ precioBase: 95000, version: 3 })
    const { solicitudes, usuario } = montar(`/prestador/servicios/${ID}`, {
      [`GET ${BASE}/${ID}`]: enSecuencia(
        () => respuestaJson(200, servicioGestion({ version: 2 })),
        () => respuestaJson(200, servicioGestion({ version: 2 })),
        () => respuestaJson(200, actualizado),
      ),
      [`PUT ${BASE}/${ID}/precio`]: () => respuestaJson(200, actualizado),
    })

    await titulo('Tour de Café en Minca')
    await usuario.click(screen.getByRole('link', { name: 'Actualizar precio' }))
    await titulo('Actualizar precio: Tour de Café en Minca')
    expect(await screen.findByText(/está Pendiente de aprobación: el cambio se verá/)).toBeInTheDocument()

    const campo = screen.getByLabelText('Precio «desde» (COP)')
    expect(campo).toHaveValue('80000')
    fireEvent.change(campo, { target: { value: '95000' } })
    await usuario.click(guardar())

    await waitFor(() => expect(ubicacionActual()).toBe(`/prestador/servicios/${ID}`))
    expect(puts(solicitudes)).toHaveLength(1)
    // C7: solo el campo de su categoría y la versión leída.
    expect(puts(solicitudes)[0].cuerpo).toEqual({ precioBase: 95000, version: 2 })
    expect(await screen.findByText('Precio actualizado.', {}, LENTO)).toHaveAttribute('role', 'status')
  })

  it('validación en el cliente: sin precio no se envía y el error queda junto al campo', async () => {
    const { solicitudes, usuario } = montar(`/prestador/servicios/${ID}/precio`, {
      [`GET ${BASE}/${ID}`]: () => respuestaJson(200, servicioGestion()),
    })
    await titulo('Actualizar precio: Tour de Café en Minca')
    fireEvent.change(screen.getByLabelText('Precio «desde» (COP)'), { target: { value: '' } })
    await usuario.click(guardar())

    const campo = screen.getByLabelText('Precio «desde» (COP)')
    expect(campo).toHaveAttribute('aria-invalid', 'true')
    expect(campo).toHaveAccessibleDescription(/Este dato es obligatorio/)
    await waitFor(() => expect(campo).toHaveFocus())
    expect(puts(solicitudes)).toHaveLength(0)
  })

  it('validación en el cliente (C10): más de 99 999 999,99 o más de 2 decimales no se envían', async () => {
    const { solicitudes, usuario } = montar(`/prestador/servicios/${ID}/precio`, {
      [`GET ${BASE}/${ID}`]: () => respuestaJson(200, servicioGestion()),
    })
    await titulo('Actualizar precio: Tour de Café en Minca')
    const campo = () => screen.getByLabelText('Precio «desde» (COP)')

    fireEvent.change(campo(), { target: { value: '100000000' } })
    await usuario.click(guardar())
    expect(campo()).toHaveAccessibleDescription(/El valor es mayor que el permitido/)

    fireEvent.change(campo(), { target: { value: '85000.505' } })
    await usuario.click(guardar())
    expect(campo()).toHaveAccessibleDescription(/El formato no es válido/)
    expect(puts(solicitudes)).toHaveLength(0)
  })

  it('doble clic: un solo PUT', async () => {
    let responder: (respuesta: Response) => void = () => {}
    const { solicitudes, usuario } = montar(`/prestador/servicios/${ID}/precio`, {
      [`GET ${BASE}/${ID}`]: () => respuestaJson(200, servicioGestion()),
      [`PUT ${BASE}/${ID}/precio`]: () => new Promise<Response>((resolver) => (responder = resolver)),
    })
    await titulo('Actualizar precio: Tour de Café en Minca')
    const formulario = guardar().closest('form') as HTMLFormElement
    fireEvent.submit(formulario)
    fireEvent.submit(formulario)
    const enviando = await screen.findByRole('button', { name: 'Guardando…' })
    expect(enviando).toBeDisabled()
    await usuario.click(enviando)

    expect(puts(solicitudes)).toHaveLength(1)
    responder(respuestaJson(200, servicioGestion({ version: 1 })))
    await waitFor(() => expect(ubicacionActual()).toBe(`/prestador/servicios/${ID}`))
  })

  it('409 SERVICIO_MODIFICADO: aviso, relectura sin perder lo escrito, «Valor actual» y reenvío con la versión nueva', async () => {
    const { solicitudes, usuario } = montar(`/prestador/servicios/${ID}/precio`, {
      [`GET ${BASE}/${ID}`]: enSecuencia(
        () => respuestaJson(200, servicioGestion({ version: 2 })),
        () => respuestaJson(200, servicioGestion({ version: 3, precioBase: 85000 })),
      ),
      [`PUT ${BASE}/${ID}/precio`]: enSecuencia(
        () => respuestaProblema(409, 'SERVICIO_MODIFICADO', { versionActual: 3 }),
        () => respuestaJson(200, servicioGestion({ version: 4, precioBase: 95000 })),
      ),
    })
    await titulo('Actualizar precio: Tour de Café en Minca')
    fireEvent.change(screen.getByLabelText('Precio «desde» (COP)'), { target: { value: '95000' } })
    await usuario.click(guardar())

    expect(await screen.findByText(/El servicio cambió mientras lo editabas/, {}, LENTO)).toBeInTheDocument()
    const campo = screen.getByLabelText('Precio «desde» (COP)')
    expect(campo).toHaveValue('95000')
    expect(campo).toHaveAccessibleDescription(/Valor actual: .*85\.000/)
    expect(ubicacionActual()).toBe(`/prestador/servicios/${ID}/precio`)

    await usuario.click(guardar())
    await waitFor(() => expect(puts(solicitudes)).toHaveLength(2))
    expect(puts(solicitudes)[1].cuerpo).toEqual({ precioBase: 95000, version: 3 })
    await waitFor(() => expect(ubicacionActual()).toBe(`/prestador/servicios/${ID}`))
  })

  it('409 ESTADO_CAMBIADO: vuelve al detalle con el aviso y sin acciones de edición', async () => {
    const { usuario } = montar(`/prestador/servicios/${ID}/precio`, {
      [`GET ${BASE}/${ID}`]: enSecuencia(
        () => respuestaJson(200, servicioGestion()),
        () => respuestaJson(200, servicioGestion({ estadoPublicacion: 'Rechazado', motivoRechazo: 'RNT vencido', version: 1 })),
      ),
      [`PUT ${BASE}/${ID}/precio`]: () => respuestaProblema(409, 'ESTADO_CAMBIADO', { estadoActual: 'Rechazado' }),
    })
    await titulo('Actualizar precio: Tour de Café en Minca')
    await usuario.click(guardar())

    await waitFor(() => expect(ubicacionActual()).toBe(`/prestador/servicios/${ID}`))
    expect(await screen.findByText('No se guardó: el servicio ahora está Rechazado y ya no se puede editar.', {}, LENTO)).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Actualizar precio' })).not.toBeInTheDocument()
  })

  it('404: «no encontrado», sin formulario', async () => {
    const { usuario } = montar(`/prestador/servicios/${ID}/precio`, {
      [`GET ${BASE}/${ID}`]: () => respuestaJson(200, servicioGestion()),
      [`PUT ${BASE}/${ID}/precio`]: () => respuestaProblema(404, 'SERVICIO_NO_ENCONTRADO'),
    })
    await titulo('Actualizar precio: Tour de Café en Minca')
    await usuario.click(guardar())

    expect(await screen.findByText('No encontramos ese servicio entre los tuyos.', {}, LENTO)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Guardar cambios' })).not.toBeInTheDocument()
  })

  it('un servicio Rechazado: el detalle no ofrece editar y la página no muestra el formulario', async () => {
    const rechazado = () => respuestaJson(200, servicioGestion({ estadoPublicacion: 'Rechazado', motivoRechazo: 'RNT vencido' }))
    montar(`/prestador/servicios/${ID}`, { [`GET ${BASE}/${ID}`]: rechazado })
    await titulo('Tour de Café en Minca')
    expect(screen.queryByRole('link', { name: 'Actualizar precio' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Editar datos comerciales' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Editar y reenviar/ })).toHaveAttribute('aria-disabled', 'true')
  })

  it.each(['Pendiente', 'Publicado'])('«Editar y reenviar» no aparece en %s (DEC-193, C11)', async (estado) => {
    montar(`/prestador/servicios/${ID}`, {
      [`GET ${BASE}/${ID}`]: () => respuestaJson(200, servicioGestion({ estadoPublicacion: estado })),
    })
    await titulo('Tour de Café en Minca')
    expect(screen.getByRole('link', { name: 'Actualizar precio' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Editar y reenviar/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Editar y reenviar/ })).not.toBeInTheDocument()
  })

  it('directo a la ruta de un Rechazado: aviso y enlace al servicio', async () => {
    montar(`/prestador/servicios/${ID}/precio`, {
      [`GET ${BASE}/${ID}`]: () => respuestaJson(200, servicioGestion({ estadoPublicacion: 'Rechazado' })),
    })
    expect(await screen.findByText('Este servicio está Rechazado y no se puede editar.', {}, LENTO)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver al servicio' })).toHaveAttribute('href', `/prestador/servicios/${ID}`)
    expect(screen.queryByRole('button', { name: 'Guardar cambios' })).not.toBeInTheDocument()
  })
})

describe('Precio de un Alojamiento (RF32)', () => {
  it('una fila por unidad, el «desde» derivado y todas las unidades en el cuerpo con la versión', async () => {
    const { solicitudes, usuario } = montar(`/prestador/servicios/${ID_HOTEL}/precio`, {
      [`GET ${BASE}/${ID_HOTEL}`]: () => respuestaJson(200, servicioGestionHotel({ estadoPublicacion: 'Publicado', version: 5 })),
      [`PUT ${BASE}/${ID_HOTEL}/precio`]: () => respuestaJson(200, servicioGestionHotel({ version: 6 })),
    })
    await titulo('Actualizar precio: Hotel Casa Tayrona')
    expect(await screen.findByText(/está Publicado: el cambio se ve de inmediato/)).toBeInTheDocument()
    // El «desde» no se escribe: es un <output> derivado (DEC-163).
    expect(screen.queryByRole('textbox', { name: 'Precio «desde» (COP)' })).not.toBeInTheDocument()
    const desde = screen.getByLabelText('Precio «desde» (COP)')
    expect(desde.tagName).toBe('OUTPUT')
    expect(desde).toHaveTextContent(/150\.000/)

    fireEvent.change(screen.getByLabelText('Precio por noche de Suite familiar'), { target: { value: '120000' } })
    expect(desde).toHaveTextContent(/120\.000/)
    await usuario.click(guardar())

    await waitFor(() => expect(puts(solicitudes)).toHaveLength(1))
    expect(puts(solicitudes)[0].cuerpo).toEqual({
      unidades: [
        { idUnidad: 'u-doble', precioNoche: 150000 },
        { idUnidad: 'u-suite', precioNoche: 120000 },
      ],
      version: 5,
    })
  })

  it('400 con índice: el error de `unidades[1].precioNoche` queda junto a la segunda unidad', async () => {
    const { usuario } = montar(`/prestador/servicios/${ID_HOTEL}/precio`, {
      [`GET ${BASE}/${ID_HOTEL}`]: () => respuestaJson(200, servicioGestionHotel()),
      [`PUT ${BASE}/${ID_HOTEL}/precio`]: () =>
        respuestaProblema(400, 'DATOS_INVALIDOS', { campos: { 'unidades[1].precioNoche': 'VALOR_MAXIMO' } }),
    })
    await titulo('Actualizar precio: Hotel Casa Tayrona')
    await usuario.click(guardar())

    const suite = screen.getByLabelText('Precio por noche de Suite familiar')
    await waitFor(() => expect(suite).toHaveAttribute('aria-invalid', 'true'))
    expect(suite).toHaveAccessibleDescription(/El valor es mayor que el permitido/)
    expect(screen.getByLabelText('Precio por noche de Doble')).not.toHaveAttribute('aria-invalid')
    await waitFor(() => expect(suite).toHaveFocus())
  })

  it('400 sin control (una unidad que ya no es del servicio): mensaje general con «Recargar»', async () => {
    const { solicitudes, usuario } = montar(`/prestador/servicios/${ID_HOTEL}/precio`, {
      [`GET ${BASE}/${ID_HOTEL}`]: () => respuestaJson(200, servicioGestionHotel()),
      [`PUT ${BASE}/${ID_HOTEL}/precio`]: () =>
        respuestaProblema(400, 'DATOS_INVALIDOS', { campos: { 'unidades[0].idUnidad': 'NO_PERMITIDO' } }),
    })
    await titulo('Actualizar precio: Hotel Casa Tayrona')
    await usuario.click(guardar())

    const alerta = await screen.findByText(/Algunos datos no son válidos/, {}, LENTO)
    await usuario.click(within(alerta.parentElement as HTMLElement).getByRole('button', { name: 'Recargar' }))
    await waitFor(() => expect(solicitudes.filter((s) => s.url === `${BASE}/${ID_HOTEL}`)).toHaveLength(2))
  })
})
