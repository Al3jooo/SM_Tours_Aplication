import { fireEvent, screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  USUARIO_PRESTADOR,
  respuestaJson,
  respuestaProblema,
  servicioGestion,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

/** WF-21 «Editar datos comerciales» del servicio (RF43, flujo B; DEC-196, DEC-199 a DEC-201). */

const ME = 'GET /api/v1/auth/me'
const ID = '6a3000000000000000000001'
const BASE = '/api/v1/catalogo/prestador/servicios'
const RUTA = `/prestador/servicios/${ID}/datos-comerciales`
const LENTO = { timeout: 15_000 }

type Manejador = (solicitud: SolicitudSimulada) => Response | Promise<Response>

function montar(ruta: string, rutas: Record<string, Manejador>) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  const simulado = simularFetch({ [ME]: () => respuestaJson(200, USUARIO_PRESTADOR), ...rutas })
  return { ...simulado, ...renderApp(ruta) }
}

function enSecuencia(...respuestas: (() => Response)[]): Manejador {
  let llamada = 0
  return () => respuestas[Math.min(llamada++, respuestas.length - 1)]()
}

const puts = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.metodo === 'PUT')
const titulo = () => screen.findByRole('heading', { level: 1, name: 'Editar datos comerciales: Tour de Café en Minca' }, LENTO)
const guardar = () => screen.getByRole('button', { name: 'Guardar cambios' })

describe('Datos comerciales del servicio (RF43)', () => {
  it('desde el detalle Publicado: el aviso de «sin nueva aprobación», la nota de los contactos y un cuerpo con solo nombre, contacto y versión', async () => {
    const publicado = (extra = {}) => respuestaJson(200, servicioGestion({ estadoPublicacion: 'Publicado', version: 7, ...extra }))
    const { solicitudes, usuario } = montar(`/prestador/servicios/${ID}`, {
      [`GET ${BASE}/${ID}`]: publicado,
      [`PUT ${BASE}/${ID}/datos-comerciales`]: () => publicado({ nombreComercial: 'Café de Minca', version: 8 }),
    })
    await screen.findByRole('heading', { level: 1, name: 'Tour de Café en Minca' }, LENTO)
    await usuario.click(screen.getByRole('link', { name: 'Editar datos comerciales' }))
    await titulo()

    expect(screen.getByText(/está Publicado: el cambio se ve de inmediato en la ficha pública, sin nueva aprobación/)).toBeInTheDocument()
    // C2: el contacto del servicio no es el correo corporativo ni el correo de acceso.
    expect(screen.getByText(/Es distinto del correo corporativo de tu perfil comercial y del correo con el que inicias sesión/)).toBeInTheDocument()
    expect(screen.getByLabelText('Teléfono / WhatsApp')).toHaveValue('+57 300 000 0301')
    expect(screen.getByLabelText('Correo electrónico')).toHaveValue('tours@cafeminca.test')

    fireEvent.change(screen.getByLabelText('Nombre comercial'), { target: { value: '  Café de Minca ' } })
    await usuario.click(guardar())

    await waitFor(() => expect(ubicacionActual()).toBe(`/prestador/servicios/${ID}`))
    // C7: solo los campos de este formulario (ni precio, ni oferta, ni RNT).
    expect(puts(solicitudes)[0].cuerpo).toEqual({
      nombreComercial: 'Café de Minca',
      contacto: { telefono: '+57 300 000 0301', correo: 'tours@cafeminca.test' },
      version: 7,
    })
    expect(await screen.findByText('Datos comerciales actualizados.', {}, LENTO)).toBeInTheDocument()
  })

  it('validación en el cliente: el correo con mala forma no se envía', async () => {
    const { solicitudes, usuario } = montar(RUTA, { [`GET ${BASE}/${ID}`]: () => respuestaJson(200, servicioGestion()) })
    await titulo()
    fireEvent.change(screen.getByLabelText('Correo electrónico'), { target: { value: 'sin-arroba' } })
    await usuario.click(guardar())

    expect(screen.getByLabelText('Correo electrónico')).toHaveAccessibleDescription(/El formato no es válido/)
    expect(puts(solicitudes)).toHaveLength(0)
  })

  it('400 del servidor junto al campo (`contacto.telefono`)', async () => {
    const { usuario } = montar(RUTA, {
      [`GET ${BASE}/${ID}`]: () => respuestaJson(200, servicioGestion()),
      [`PUT ${BASE}/${ID}/datos-comerciales`]: () =>
        respuestaProblema(400, 'DATOS_INVALIDOS', { campos: { 'contacto.telefono': 'LONGITUD_MAXIMA' } }),
    })
    await titulo()
    await usuario.click(guardar())

    const telefono = screen.getByLabelText('Teléfono / WhatsApp')
    await waitFor(() => expect(telefono).toHaveAttribute('aria-invalid', 'true'))
    expect(telefono).toHaveAccessibleDescription(/Usa como máximo 20 caracteres/)
  })

  it('409 SERVICIO_MODIFICADO: conserva lo escrito, marca el valor actual y reenvía con la versión nueva', async () => {
    const { solicitudes, usuario } = montar(RUTA, {
      [`GET ${BASE}/${ID}`]: enSecuencia(
        () => respuestaJson(200, servicioGestion({ version: 1 })),
        () => respuestaJson(200, servicioGestion({ version: 2, nombreComercial: 'Tour Cafetero' })),
      ),
      [`PUT ${BASE}/${ID}/datos-comerciales`]: enSecuencia(
        () => respuestaProblema(409, 'SERVICIO_MODIFICADO', { versionActual: 2 }),
        () => respuestaJson(200, servicioGestion({ version: 3 })),
      ),
    })
    await titulo()
    fireEvent.change(screen.getByLabelText('Nombre comercial'), { target: { value: 'Café de Minca' } })
    await usuario.click(guardar())

    expect(await screen.findByText(/El servicio cambió mientras lo editabas/, {}, LENTO)).toBeInTheDocument()
    const nombre = screen.getByLabelText('Nombre comercial')
    expect(nombre).toHaveValue('Café de Minca')
    expect(nombre).toHaveAccessibleDescription(/Valor actual: Tour Cafetero/)
    // Lo que no cambió no lleva «Valor actual».
    expect(screen.getByLabelText('Correo electrónico')).not.toHaveAccessibleDescription(/Valor actual/)

    await usuario.click(guardar())
    await waitFor(() => expect(puts(solicitudes)).toHaveLength(2))
    expect(puts(solicitudes)[1].cuerpo).toMatchObject({ nombreComercial: 'Café de Minca', version: 2 })
  })

  it('409 SERVICIO_MODIFICADO cuya relectura ya está Rechazado: vuelve al detalle con el aviso (DEC-201)', async () => {
    const { usuario } = montar(RUTA, {
      [`GET ${BASE}/${ID}`]: enSecuencia(
        () => respuestaJson(200, servicioGestion()),
        () => respuestaJson(200, servicioGestion({ estadoPublicacion: 'Rechazado', motivoRechazo: 'RNT vencido', version: 1 })),
      ),
      [`PUT ${BASE}/${ID}/datos-comerciales`]: () => respuestaProblema(409, 'SERVICIO_MODIFICADO', { versionActual: 1 }),
    })
    await titulo()
    await usuario.click(guardar())

    await waitFor(() => expect(ubicacionActual()).toBe(`/prestador/servicios/${ID}`))
    expect(await screen.findByText('No se guardó: el servicio ahora está Rechazado y ya no se puede editar.', {}, LENTO)).toBeInTheDocument()
  })

  it('«Descartar cambios» vuelve a los valores guardados', async () => {
    const { usuario } = montar(RUTA, { [`GET ${BASE}/${ID}`]: () => respuestaJson(200, servicioGestion()) })
    await titulo()
    fireEvent.change(screen.getByLabelText('Nombre comercial'), { target: { value: 'Otro' } })
    await usuario.click(screen.getByRole('button', { name: 'Descartar cambios' }))
    expect(screen.getByLabelText('Nombre comercial')).toHaveValue('Tour de Café en Minca')
  })
})
