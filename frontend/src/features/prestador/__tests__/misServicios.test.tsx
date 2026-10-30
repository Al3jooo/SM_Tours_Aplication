import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  USUARIO_ADMIN,
  USUARIO_PRESTADOR,
  USUARIO_TURISTA,
  paginaOferta,
  paginaServicios,
  respuestaJson,
  respuestaProblema,
  servicioGestion,
  servicioResumen,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

const ME = 'GET /api/v1/auth/me'
const LISTA = 'GET /api/v1/catalogo/prestador/servicios'
const ID = '6a3000000000000000000001'
const LENTO = { timeout: 15_000 }

function conSesion(usuario: object, rutas: Parameters<typeof simularFetch>[0] = {}) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  return simularFetch({ [ME]: () => respuestaJson(200, usuario), ...rutas })
}

const consultasLista = (solicitudes: SolicitudSimulada[]) =>
  solicitudes.filter((s) => s.url.startsWith('/api/v1/catalogo/prestador/servicios?'))

const RECHAZADO = servicioResumen({
  id: '6a3000000000000000000002',
  nombreComercial: 'Kayak Taganga',
  categoria: 'AlquilerEquipo',
  estadoPublicacion: 'Rechazado',
  motivoRechazo: 'El RNT no está vigente',
})
const PUBLICADO = servicioResumen({ id: '6a3000000000000000000003', nombreComercial: 'Hotel Casa Tayrona', categoria: 'Alojamiento', estadoPublicacion: 'Publicado' })

describe('Rutas del Prestador (DEC-161)', () => {
  it('el menú del Prestador lleva a «Mis servicios» y a «Registrar servicio»', async () => {
    conSesion(USUARIO_PRESTADOR, { 'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([])) })
    const { usuario } = renderApp('/')
    await usuario.click(await screen.findByRole('button', { name: 'Menú de usuario de Casa Tayrona SAS' }))

    const menu = within(screen.getByRole('menu'))
    expect(menu.getByRole('menuitem', { name: 'Mis servicios' })).toHaveAttribute('href', '/prestador/servicios')
    expect(menu.getByRole('menuitem', { name: 'Registrar servicio' })).toHaveAttribute('href', '/prestador/servicios/nuevo')
  })

  it.each([
    ['Turista', USUARIO_TURISTA],
    ['Administrador', USUARIO_ADMIN],
  ])('el menú del %s no tiene las opciones del Prestador', async (_rol, usuarioActual) => {
    conSesion(usuarioActual, { 'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([])) })
    const { usuario } = renderApp('/')
    await usuario.click(await screen.findByRole('button', { name: `Menú de usuario de ${usuarioActual.nombreVisible}` }))

    const menu = within(screen.getByRole('menu'))
    expect(menu.queryByRole('menuitem', { name: 'Mis servicios' })).not.toBeInTheDocument()
    expect(menu.queryByRole('menuitem', { name: 'Registrar servicio' })).not.toBeInTheDocument()
  })

  it.each(['/prestador/servicios', '/prestador/servicios/nuevo', `/prestador/servicios/${ID}`])(
    'un Turista en %s ve «Sin permiso» y no llama a la API del Prestador',
    async (ruta) => {
      const { solicitudes } = conSesion(USUARIO_TURISTA)
      renderApp(ruta)

      expect(await screen.findByRole('heading', { name: 'Sin permiso' })).toBeInTheDocument()
      expect(solicitudes.some((s) => s.url.startsWith('/api/v1/catalogo/prestador'))).toBe(false)
    },
  )

  it.each(['/prestador/servicios', '/prestador/servicios/nuevo'])('sin sesión, %s lleva al login', async (ruta) => {
    simularFetch({})
    renderApp(ruta)

    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
  })
})

describe('WF-18 «Mis servicios» (DEC-169)', () => {
  it('el filtro y la página van en la URL; la API recibe la página desde 0', async () => {
    const { solicitudes } = conSesion(USUARIO_PRESTADOR, {
      [LISTA]: ({ url }) =>
        respuestaJson(200, url.includes('estado=Publicado') ? paginaServicios([PUBLICADO]) : paginaServicios([RECHAZADO], 1, 11)),
    })
    const { usuario } = renderApp('/prestador/servicios?estado=Rechazado&pagina=2')

    const tabla = await screen.findByRole('table', {}, LENTO)
    const consulta = new URL(consultasLista(solicitudes)[0].url, 'http://x').searchParams
    expect(consulta.get('estado')).toBe('Rechazado')
    expect(consulta.get('pagina')).toBe('1')
    expect(consultasLista(solicitudes)[0].cabeceras.Authorization).toBe('Bearer token-guardado')

    // Rechazado: estado en texto y su motivo en la celda.
    const fila = within(tabla).getByRole('row', { name: /Kayak Taganga/ })
    expect(within(fila).getByText('Rechazado')).toBeInTheDocument()
    expect(within(fila).getByText('Motivo: El RNT no está vigente')).toBeInTheDocument()
    expect(within(fila).queryByRole('link', { name: /ficha pública/ })).not.toBeInTheDocument()
    expect(within(fila).getByRole('link', { name: 'Ver detalle de Kayak Taganga' })).toHaveAttribute(
      'href',
      '/prestador/servicios/6a3000000000000000000002',
    )
    expect(screen.getByRole('status', { name: '' })).toHaveTextContent('11 servicios')

    await usuario.click(screen.getByRole('button', { name: 'Página 1' }))
    expect(ubicacionActual()).toBe('/prestador/servicios?estado=Rechazado')

    await usuario.selectOptions(screen.getByLabelText('Estado'), 'Publicado')
    expect(ubicacionActual()).toBe('/prestador/servicios?estado=Publicado')
    const publicado = await screen.findByRole('row', { name: /Hotel Casa Tayrona/ })
    expect(within(publicado).getByText('Publicado')).toBeInTheDocument()
    expect(within(publicado).getByRole('link', { name: 'Ver la ficha pública de Hotel Casa Tayrona' })).toHaveAttribute(
      'href',
      '/servicios/6a3000000000000000000003',
    )
  })

  it('un Pendiente se lee «Pendiente de aprobación» (N21)', async () => {
    conSesion(USUARIO_PRESTADOR, { [LISTA]: () => respuestaJson(200, paginaServicios([servicioResumen()])) })
    renderApp('/prestador/servicios')

    const fila = await screen.findByRole('row', { name: /Tour de Café en Minca/ }, LENTO)
    expect(within(fila).getByText('Pendiente de aprobación')).toBeInTheDocument()
    expect(within(fila).getByText('Tours')).toBeInTheDocument()
  })

  it('un filtro que no es válido se normaliza en la URL', async () => {
    const { solicitudes } = conSesion(USUARIO_PRESTADOR, { [LISTA]: () => respuestaJson(200, paginaServicios([servicioResumen()])) })
    renderApp('/prestador/servicios?estado=Borrador&pagina=0')

    await screen.findByRole('table', {}, LENTO)
    await waitFor(() => expect(ubicacionActual()).toBe('/prestador/servicios'))
    expect(consultasLista(solicitudes).every((s) => !s.url.includes('estado='))).toBe(true)
  })

  it('sin servicios: el vacío lleva a registrar uno', async () => {
    conSesion(USUARIO_PRESTADOR, { [LISTA]: () => respuestaJson(200, paginaServicios([])) })
    renderApp('/prestador/servicios')

    expect(await screen.findByText('Aún no registraste servicios.', {}, LENTO)).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: 'Registrar nuevo servicio' })[0]).toHaveAttribute(
      'href',
      '/prestador/servicios/nuevo',
    )
  })

  it('con el catálogo caído, mensaje y «Reintentar»', async () => {
    let llamadas = 0
    conSesion(USUARIO_PRESTADOR, {
      [LISTA]: () => {
        llamadas += 1
        return llamadas === 1
          ? respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE', { servicio: 'ms-catalog' })
          : respuestaJson(200, paginaServicios([servicioResumen()]))
      },
    })
    const { usuario } = renderApp('/prestador/servicios')

    const alerta = await screen.findByRole('alert', {}, LENTO)
    expect(alerta).toHaveTextContent('El catálogo no está disponible en este momento.')
    await usuario.click(within(alerta).getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByRole('table')).toBeInTheDocument()
  })
})

describe('WF-18, detalle propio', () => {
  it('un Rechazado muestra la fecha de revisión, el motivo y los datos enviados; «Editar y reenviar» deshabilitado', async () => {
    conSesion(USUARIO_PRESTADOR, {
      [`GET /api/v1/catalogo/prestador/servicios/${ID}`]: () =>
        respuestaJson(
          200,
          servicioGestion({
            estadoPublicacion: 'Rechazado',
            motivoRechazo: 'El RNT no está vigente',
            fechaRevision: '2026-10-29T15:00:00Z',
          }),
        ),
    })
    renderApp(`/prestador/servicios/${ID}`)

    expect(await screen.findByRole('heading', { level: 1, name: 'Tour de Café en Minca' }, LENTO)).toBeInTheDocument()
    expect(screen.getByText('Rechazado')).toBeInTheDocument()
    expect(screen.getByText('Revisado el').nextElementSibling).toHaveTextContent('29/10/2026')
    const motivo = screen.getByRole('region', { name: 'Motivo del rechazo' })
    expect(motivo).toHaveTextContent('El RNT no está vigente')
    // Los datos tal como se enviaron, con el RNT y los días traducidos.
    expect(screen.getByText('RNT-00000')).toBeInTheDocument()
    expect(screen.getByText('Lunes, Sábado')).toBeInTheDocument()
    expect(screen.getByText('07:00, 13:30')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Editar y reenviar ?\(No disponible\)$/ })).toHaveAttribute(
      'aria-disabled',
      'true',
    )
    expect(screen.queryByRole('link', { name: 'Ver ficha pública' })).not.toBeInTheDocument()
  })

  it('sin condiciones de accesibilidad (null) la página no falla y el dato queda «sin datos» (O19)', async () => {
    conSesion(USUARIO_PRESTADOR, {
      [`GET /api/v1/catalogo/prestador/servicios/${ID}`]: () =>
        respuestaJson(200, servicioGestion({ condicionesAccesibilidad: null })),
    })
    renderApp(`/prestador/servicios/${ID}`)

    expect(await screen.findByRole('heading', { level: 1, name: 'Tour de Café en Minca' }, LENTO)).toBeInTheDocument()
    const etiqueta = screen.getByText('Condiciones de accesibilidad', { selector: 'dt' })
    expect(etiqueta.nextElementSibling).toHaveTextContent('sin datos')
  })

  it('un servicio ajeno o inexistente (404) se muestra como no encontrado, sin «Reintentar»', async () => {
    conSesion(USUARIO_PRESTADOR, {
      [`GET /api/v1/catalogo/prestador/servicios/${ID}`]: () => respuestaProblema(404, 'SERVICIO_NO_ENCONTRADO'),
    })
    renderApp(`/prestador/servicios/${ID}`)

    const alerta = await screen.findByRole('alert', {}, LENTO)
    expect(alerta).toHaveTextContent('No encontramos ese servicio entre los tuyos.')
    expect(within(alerta).queryByRole('button')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Volver a Mis servicios/ })).toHaveAttribute('href', '/prestador/servicios')
  })
})
