import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  USUARIO_ADMIN,
  paginaServicios,
  respuestaJson,
  respuestaProblema,
  servicioAdmin,
  servicioAdminDe,
  servicioCola,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { leafletSimulado, mapaVivo } from '../../../test/leafletSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

/**
 * WF-19, revisión y decisión (CU «Aprobar o rechazar servicio», «Justificar rechazo de servicio»; RF21; DEC-176 a
 * DEC-180).
 */

const ME = 'GET /api/v1/auth/me'
const BASE = '/api/v1/catalogo/admin/servicios'
const ID = '6a3000000000000000000001'
const DETALLE = `GET ${BASE}/${ID}`
const DECISION = `POST ${BASE}/${ID}/aprobacion`
const COLA = `GET ${BASE}`
const LENTO = { timeout: 15_000 }

type Manejador = (solicitud: SolicitudSimulada) => Response | Promise<Response>

const decisiones = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.metodo === 'POST')
const consultasCola = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.url.startsWith(`${BASE}?`))

function montar(ruta: string, rutas: Record<string, Manejador> = {}) {
  sessionStorage.setItem('samaria360.token', 'token-admin')
  const simulado = simularFetch({
    [ME]: () => respuestaJson(200, USUARIO_ADMIN),
    [DETALLE]: () => respuestaJson(200, servicioAdmin()),
    [COLA]: () => respuestaJson(200, paginaServicios([servicioCola()])),
    ...rutas,
  })
  return { ...simulado, ...renderApp(ruta) }
}

/** Valor de un dato (`<dt>` → `<dd>`) dentro de un contenedor. */
function valor(etiqueta: string, contenedor: HTMLElement = document.body) {
  const dt = within(contenedor).getByText(etiqueta, { selector: 'dt' })
  return dt.nextElementSibling as HTMLElement
}

const titulo = (nombre: string, categoria: string) =>
  screen.findByRole('heading', { level: 1, name: `Revisar: ${nombre} · ${categoria}` }, LENTO)

/** El anuncio está una sola vez, dentro de la región `aria-live` de la cola. */
async function anunciado(texto: string) {
  const aviso = await screen.findByText(texto, {}, LENTO)
  expect(aviso.closest('[aria-live="polite"]')).not.toBeNull()
  expect(screen.getAllByText(texto)).toHaveLength(1)
}

describe('Revisión por categoría (DEC-176)', () => {
  it('Tour Pendiente: estado, prestador (id, RNT, cuenta), datos enviados, mapa de solo lectura y acciones', async () => {
    montar(`/admin/servicios/${ID}`)

    const h1 = await titulo('Tour de Café en Minca', 'Tours')
    await waitFor(() => expect(h1).toHaveFocus())
    expect(valor('Estado')).toHaveTextContent('Pendiente de aprobación')
    expect(valor('Registrado el')).toHaveTextContent('28/10/2026')

    const prestador = screen.getByRole('region', { name: 'Prestador' })
    expect(valor('Identificador', prestador)).toHaveTextContent('00000000-0000-4000-8000-000000000002')
    expect(valor('RNT', prestador)).toHaveTextContent('RNT-00000')
    expect(valor('Cuenta', prestador)).toHaveTextContent('Activo')
    expect(valor('Prestador activo', prestador)).toHaveTextContent('Sí')
    expect(screen.queryByText(/quedará Publicado pero oculto/)).not.toBeInTheDocument()

    const datos = screen.getByRole('region', { name: 'Datos enviados' })
    expect(valor('Punto de encuentro', datos)).toHaveTextContent('Iglesia de Minca')
    expect(within(datos).getByText('Lunes, Sábado')).toBeInTheDocument()
    expect(within(datos).getByText('07:00, 13:30')).toBeInTheDocument()
    expect(valor('Ubicación en el mapa', datos)).toHaveTextContent('Latitud 11.1432, longitud -74.1121')

    const mapa = mapaVivo()
    expect(mapa.marcadores()).toHaveLength(1)
    expect(mapa.marcadores()[0].posicion).toEqual([11.1432, -74.1121])
    expect(mapa.marcadores()[0].opciones.draggable).toBeFalsy()
    expect(mapa.teselas()[0].opciones.attribution).toContain('OpenStreetMap')
    expect(screen.getByRole('region', { name: 'Ubicación de Tour de Café en Minca en el mapa' })).toBeInTheDocument()

    // Sin la sección «Revisión» mientras está Pendiente.
    expect(screen.queryByRole('region', { name: 'Revisión' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Aprobar → Publicado' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Rechazar' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver' })).toHaveAttribute('href', '/admin/servicios')
  })

  it('Alojamiento: unidades, servicios incluidos y días de operación', async () => {
    montar(`/admin/servicios/${ID}`, { [DETALLE]: () => respuestaJson(200, servicioAdminDe('Alojamiento')) })

    await titulo('Hotel Casa Tayrona', 'Alojamiento')
    const datos = screen.getByRole('region', { name: 'Datos enviados' })
    const unidades = within(datos).getAllByRole('listitem').map((item) => item.textContent)
    expect(unidades).toHaveLength(2)
    expect(unidades[0]).toMatch(/^Doble · Doble · 3 habitaciones · hasta 2 huéspedes · .*150\.000.* por noche$/)
    expect(unidades[1]).toMatch(/^Suite familiar · Suite · 3 habitaciones · hasta 4 huéspedes · .*260\.000,50.* por noche$/)
    expect(valor('Servicios incluidos', datos)).toHaveTextContent(/WiFi/)
    expect(valor('Días de operación', datos)).toHaveTextContent('Lunes, Martes, Miércoles, Jueves, Viernes, Sábado, Domingo')
    expect(within(datos).queryByText('Salidas', { selector: 'dt' })).not.toBeInTheDocument()
  })

  it('Gastronomía: datos de la categoría, sin oferta', async () => {
    montar(`/admin/servicios/${ID}`, { [DETALLE]: () => respuestaJson(200, servicioAdminDe('Gastronomia')) })

    await titulo('Mirador Gourmet', 'Gastronomía')
    const datos = screen.getByRole('region', { name: 'Datos enviados' })
    expect(valor('Tipo de cocina', datos)).toHaveTextContent('Caribeña')
    expect(within(datos).queryByText('Días de operación', { selector: 'dt' })).not.toBeInTheDocument()
  })

  it('Alquiler de equipos: datos de la categoría, sin oferta', async () => {
    montar(`/admin/servicios/${ID}`, { [DETALLE]: () => respuestaJson(200, servicioAdminDe('AlquilerEquipo')) })

    await titulo('Kayaks Taganga', 'Alquiler de equipos')
    const datos = screen.getByRole('region', { name: 'Datos enviados' })
    expect(within(datos).getByText('Kayak doble')).toBeInTheDocument()
    expect(within(datos).getByText('Chaleco incluido')).toBeInTheDocument()
    expect(within(datos).queryByText('Días de operación', { selector: 'dt' })).not.toBeInTheDocument()
  })

  it('con valores nulos (sin contacto, accesibilidad, oferta, coordenadas, RNT ni cuenta) no falla y no dibuja el mapa', async () => {
    const sinDatos = servicioAdmin({
      descripcion: null,
      direccion: null,
      contacto: null,
      condicionesAccesibilidad: null,
      oferta: null,
      coordenadas: null,
      rnt: null,
      detalleCategoria: null,
      precioBase: null,
      estadoCuentaPrestador: undefined,
    })
    montar(`/admin/servicios/${ID}`, { [DETALLE]: () => respuestaJson(200, sinDatos) })

    await titulo('Tour de Café en Minca', 'Tours')
    const prestador = screen.getByRole('region', { name: 'Prestador' })
    expect(valor('Cuenta', prestador)).toHaveTextContent('sin datos')
    expect(valor('RNT', prestador)).toHaveTextContent('sin datos')
    const datos = screen.getByRole('region', { name: 'Datos enviados' })
    expect(valor('Ubicación en el mapa', datos)).toHaveTextContent('sin datos')
    expect(leafletSimulado.mapas).toHaveLength(0)
  })

  it('un prestador no activo: aviso de que quedará oculto aunque se apruebe (RF22, DEC-180)', async () => {
    montar(`/admin/servicios/${ID}`, {
      [DETALLE]: () => respuestaJson(200, servicioAdmin({ prestadorActivo: false, estadoCuentaPrestador: 'Inactivo' })),
    })

    await titulo('Tour de Café en Minca', 'Tours')
    expect(
      screen.getByText(
        'La cuenta del prestador no está activa: si apruebas el servicio, quedará Publicado pero oculto en el catálogo público.',
      ),
    ).toBeInTheDocument()
    const prestador = screen.getByRole('region', { name: 'Prestador' })
    expect(valor('Cuenta', prestador)).toHaveTextContent('Inactivo')
    expect(valor('Prestador activo', prestador)).toHaveTextContent('No')
    // Aprobar sigue disponible (FA6 del CU).
    expect(screen.getByRole('button', { name: 'Aprobar → Publicado' })).not.toHaveAttribute('aria-disabled')
  })
})

describe('Estados finales (DEC-179)', () => {
  it('Publicado: sin acciones de decisión; muestra la fecha y el revisor (su id)', async () => {
    montar(`/admin/servicios/${ID}`, {
      [DETALLE]: () =>
        respuestaJson(
          200,
          servicioAdmin({
            estadoPublicacion: 'Publicado',
            fechaRevision: '2026-10-29T15:00:00Z',
            revisadoPor: USUARIO_ADMIN.idUsuario,
          }),
        ),
    })

    await titulo('Tour de Café en Minca', 'Tours')
    const revision = screen.getByRole('region', { name: 'Revisión' })
    expect(valor('Revisado el', revision)).toHaveTextContent('29/10/2026')
    expect(valor('Revisado por (identificador)', revision)).toHaveTextContent(USUARIO_ADMIN.idUsuario)
    expect(within(revision).queryByText('Motivo del rechazo')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Aprobar → Publicado' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Rechazar' })).not.toBeInTheDocument()
  })

  it('Rechazado: sin acciones de decisión; muestra el motivo', async () => {
    montar(`/admin/servicios/${ID}`, {
      [DETALLE]: () =>
        respuestaJson(
          200,
          servicioAdmin({
            estadoPublicacion: 'Rechazado',
            motivoRechazo: 'Las fotos no corresponden al lugar',
            fechaRevision: '2026-10-29T15:00:00Z',
            revisadoPor: USUARIO_ADMIN.idUsuario,
          }),
        ),
    })

    await titulo('Tour de Café en Minca', 'Tours')
    const revision = screen.getByRole('region', { name: 'Revisión' })
    expect(valor('Motivo del rechazo', revision)).toHaveTextContent('Las fotos no corresponden al lugar')
    expect(screen.queryByRole('button', { name: 'Aprobar → Publicado' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Rechazar' })).not.toBeInTheDocument()
  })
})

describe('Aprobar (DEC-177)', () => {
  it('desde la cola: envía {decision: "Aprobar"}, vuelve a la cola con el aviso y la cola ya no tiene el servicio', async () => {
    let consultas = 0
    const { usuario, solicitudes } = montar('/admin/servicios', {
      [COLA]: () => {
        consultas += 1
        return respuestaJson(200, paginaServicios(consultas === 1 ? [servicioCola()] : []))
      },
      [DECISION]: () =>
        respuestaJson(
          200,
          servicioAdmin({ estadoPublicacion: 'Publicado', fechaRevision: '2026-10-30T15:00:00Z', revisadoPor: USUARIO_ADMIN.idUsuario }),
        ),
    })

    await usuario.click(await screen.findByRole('link', { name: 'Revisar Tour de Café en Minca' }, LENTO))
    await titulo('Tour de Café en Minca', 'Tours')
    await usuario.click(screen.getByRole('button', { name: 'Aprobar → Publicado' }))

    await anunciado('Se aprobó «Tour de Café en Minca»: ahora está Publicado.')
    expect(ubicacionActual()).toBe('/admin/servicios')
    expect(decisiones(solicitudes)).toHaveLength(1)
    expect(decisiones(solicitudes)[0].cuerpo).toEqual({ decision: 'Aprobar' })
    expect(await screen.findByText('No hay servicios pendientes de aprobación.')).toBeInTheDocument()
    expect(consultasCola(solicitudes)).toHaveLength(2)
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('heading', { level: 2, name: '0 servicios' })).toHaveFocus())
  })

  it('volver a la cola conserva el filtro y la página con los que se abrió la revisión', async () => {
    const { usuario } = montar('/admin/servicios?pagina=2', {
      [COLA]: () => respuestaJson(200, paginaServicios([servicioCola()], 1, 15)),
    })

    await usuario.click(await screen.findByRole('link', { name: 'Revisar Tour de Café en Minca' }, LENTO))
    await titulo('Tour de Café en Minca', 'Tours')
    expect(screen.getByRole('link', { name: 'Volver' })).toHaveAttribute('href', '/admin/servicios?pagina=2')
  })

  it('doble clic: un solo POST', async () => {
    let responder: (respuesta: Response) => void = () => {}
    const { usuario, solicitudes } = montar(`/admin/servicios/${ID}`, {
      [DECISION]: () => new Promise<Response>((resolver) => (responder = resolver)),
    })

    const aprobar = await screen.findByRole('button', { name: 'Aprobar → Publicado' }, LENTO)
    await usuario.dblClick(aprobar)
    await usuario.click(aprobar)
    expect(decisiones(solicitudes)).toHaveLength(1)
    expect(screen.getByRole('button', { name: 'Guardando…' })).toHaveAttribute('aria-disabled', 'true')

    responder(respuestaJson(200, servicioAdmin({ estadoPublicacion: 'Publicado' })))
    await anunciado('Se aprobó «Tour de Café en Minca»: ahora está Publicado.')
    expect(decisiones(solicitudes)).toHaveLength(1)
  })

  it('un error de red se muestra en la revisión, que sigue ahí, y se puede volver a intentar', async () => {
    let intentos = 0
    const { usuario, solicitudes } = montar(`/admin/servicios/${ID}`, {
      [DECISION]: () => {
        intentos += 1
        if (intentos === 1) throw new TypeError('Failed to fetch')
        return respuestaJson(200, servicioAdmin({ estadoPublicacion: 'Publicado' }))
      },
    })

    await usuario.click(await screen.findByRole('button', { name: 'Aprobar → Publicado' }, LENTO))
    expect(await screen.findByRole('alert')).toHaveTextContent('No fue posible conectar con el servidor.')
    expect(ubicacionActual()).toBe(`/admin/servicios/${ID}`)

    await usuario.click(screen.getByRole('button', { name: 'Aprobar → Publicado' }))
    await anunciado('Se aprobó «Tour de Café en Minca»: ahora está Publicado.')
    expect(decisiones(solicitudes)).toHaveLength(2)
  })
})

describe('Rechazar con justificación («Justificar rechazo de servicio», DEC-177)', () => {
  it('sin justificación no se envía; con ella, el cuerpo lleva el texto recortado y la cola anuncia el rechazo', async () => {
    const { usuario, solicitudes } = montar(`/admin/servicios/${ID}`, {
      [DECISION]: () =>
        respuestaJson(
          200,
          servicioAdmin({ estadoPublicacion: 'Rechazado', motivoRechazo: 'Las fotos no corresponden al lugar' }),
        ),
    })

    await usuario.click(await screen.findByRole('button', { name: 'Rechazar' }, LENTO))
    const dialogo = screen.getByRole('dialog', { name: 'Rechazar servicio «Tour de Café en Minca»' })
    expect(dialogo).toHaveTextContent('el prestador verá la justificación en «Mis servicios»')
    const campo = within(dialogo).getByLabelText('Justificación (obligatoria)')
    await waitFor(() => expect(campo).toHaveFocus())
    expect(within(dialogo).getByText('0 de 500 caracteres')).toBeInTheDocument()

    const confirmar = within(dialogo).getByRole('button', { name: 'Confirmar rechazo' })
    expect(confirmar).toHaveAttribute('aria-disabled', 'true')
    await usuario.click(confirmar)
    expect(decisiones(solicitudes)).toHaveLength(0)

    await usuario.type(campo, '  Las fotos no corresponden al lugar  ')
    await usuario.click(confirmar)

    await anunciado('Se rechazó «Tour de Café en Minca»: ahora está Rechazado.')
    expect(decisiones(solicitudes)).toHaveLength(1)
    expect(decisiones(solicitudes)[0].cuerpo).toEqual({
      decision: 'Rechazar',
      justificacion: 'Las fotos no corresponden al lugar',
    })
  })

  it('un 400 DATOS_INVALIDOS de la justificación se muestra junto al campo y el diálogo sigue abierto', async () => {
    const { usuario } = montar(`/admin/servicios/${ID}`, {
      [DECISION]: () => respuestaProblema(400, 'DATOS_INVALIDOS', { campos: { justificacion: 'REQUERIDO' } }),
    })

    await usuario.click(await screen.findByRole('button', { name: 'Rechazar' }, LENTO))
    const dialogo = screen.getByRole('dialog')
    const campo = within(dialogo).getByLabelText('Justificación (obligatoria)')
    await usuario.type(campo, 'x')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Confirmar rechazo' }))

    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('Escribe la justificación del rechazo.')
    expect(campo).toHaveAttribute('aria-invalid', 'true')
    expect(ubicacionActual()).toBe(`/admin/servicios/${ID}`)
  })

  it('Cancelar cierra el diálogo y el foco vuelve a «Rechazar»', async () => {
    const { usuario, solicitudes } = montar(`/admin/servicios/${ID}`)

    const rechazar = await screen.findByRole('button', { name: 'Rechazar' }, LENTO)
    await usuario.click(rechazar)
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(rechazar).toHaveFocus()
    expect(decisiones(solicitudes)).toHaveLength(0)
  })
})

describe('Conflictos y errores (DEC-178)', () => {
  it('409 ESTADO_CAMBIADO: vuelve a la cola, avisa con el estado actual y la recarga (DEC-52)', async () => {
    const { usuario, solicitudes } = montar(`/admin/servicios/${ID}`, {
      [DECISION]: () => respuestaProblema(409, 'ESTADO_CAMBIADO', { estadoActual: 'Publicado' }),
    })

    await usuario.click(await screen.findByRole('button', { name: 'Aprobar → Publicado' }, LENTO))

    await anunciado(
      '«Tour de Café en Minca» ya fue revisado por otro administrador (ahora está Publicado); se recargó la lista.',
    )
    expect(ubicacionActual()).toBe('/admin/servicios')
    await waitFor(() => expect(consultasCola(solicitudes)).toHaveLength(1))
  })

  it('409 ESTADO_CAMBIADO al rechazar: se cierra el diálogo y se vuelve a la cola', async () => {
    const { usuario } = montar(`/admin/servicios/${ID}`, {
      [DECISION]: () => respuestaProblema(409, 'ESTADO_CAMBIADO', { estadoActual: 'Rechazado' }),
    })

    await usuario.click(await screen.findByRole('button', { name: 'Rechazar' }, LENTO))
    await usuario.type(within(screen.getByRole('dialog')).getByLabelText('Justificación (obligatoria)'), 'No cumple')
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirmar rechazo' }))

    await anunciado(
      '«Tour de Café en Minca» ya fue revisado por otro administrador (ahora está Rechazado); se recargó la lista.',
    )
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('404 SERVICIO_NO_ENCONTRADO al decidir: mensaje y vuelta a la cola', async () => {
    const { usuario } = montar(`/admin/servicios/${ID}`, {
      [DECISION]: () => respuestaProblema(404, 'SERVICIO_NO_ENCONTRADO'),
    })

    await usuario.click(await screen.findByRole('button', { name: 'Aprobar → Publicado' }, LENTO))
    await anunciado('El servicio ya no existe; se recargó la lista.')
    expect(ubicacionActual()).toBe('/admin/servicios')
  })

  it('404 SERVICIO_NO_ENCONTRADO al abrir la revisión: mensaje y vuelta a la cola', async () => {
    montar(`/admin/servicios/${ID}`, { [DETALLE]: () => respuestaProblema(404, 'SERVICIO_NO_ENCONTRADO') })

    await anunciado('El servicio ya no existe; se recargó la lista.')
    expect(ubicacionActual()).toBe('/admin/servicios')
  })

  it('401 SESION_INVALIDADA: cierra la sesión y el login dice por qué', async () => {
    const { usuario } = montar(`/admin/servicios/${ID}`, {
      [DECISION]: () => respuestaProblema(401, 'SESION_INVALIDADA'),
    })

    await usuario.click(await screen.findByRole('button', { name: 'Aprobar → Publicado' }, LENTO))

    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
    expect(
      await screen.findByText('Tu sesión se cerró porque cambió el estado o los permisos de tu cuenta.'),
    ).toBeInTheDocument()
    expect(sessionStorage.getItem('samaria360.token')).toBeNull()
  })

  it('403 ACCESO_DENEGADO: «Sin permiso» sin cerrar la sesión', async () => {
    montar(`/admin/servicios/${ID}`, { [DETALLE]: () => respuestaProblema(403, 'ACCESO_DENEGADO') })

    expect(await screen.findByRole('heading', { level: 1, name: 'Sin permiso' }, LENTO)).toBeInTheDocument()
    expect(sessionStorage.getItem('samaria360.token')).toBe('token-admin')
  })

  it('el catálogo caído al abrir la revisión: mensaje y «Reintentar»', async () => {
    let intentos = 0
    const { usuario } = montar(`/admin/servicios/${ID}`, {
      [DETALLE]: () => {
        intentos += 1
        return intentos === 1
          ? respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE', { servicio: 'ms-catalog' })
          : respuestaJson(200, servicioAdmin())
      },
    })

    const alerta = await screen.findByRole('alert', {}, LENTO)
    expect(alerta).toHaveTextContent('El catálogo no está disponible en este momento.')
    await usuario.click(within(alerta).getByRole('button', { name: 'Reintentar' }))
    await titulo('Tour de Café en Minca', 'Tours')
  })
})
