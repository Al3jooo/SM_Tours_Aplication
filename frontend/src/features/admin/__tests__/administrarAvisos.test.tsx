import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  AVISO_CIERRE,
  AVISO_CLIMA,
  AVISO_INACTIVO,
  USUARIO_ADMIN,
  USUARIO_PRESTADOR,
  USUARIO_TURISTA,
  listaElementos,
  paginaAdmin,
  respuestaJson,
  respuestaProblema,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

const ME = 'GET /api/v1/auth/me'
const LISTADO = 'GET /api/v1/catalogo/admin/avisos'
const BANNER = 'GET /api/v1/catalogo/avisos'
const BASE = '/api/v1/catalogo/admin/avisos'
const RUTA = '/admin/avisos'

type Manejador = (solicitud: SolicitudSimulada) => Response | Promise<Response>

function montar(rutas: Record<string, Manejador> = {}, ruta = RUTA) {
  sessionStorage.setItem('samaria360.token', 'token-admin')
  const simulado = simularFetch({
    [ME]: () => respuestaJson(200, USUARIO_ADMIN),
    [LISTADO]: () => respuestaJson(200, paginaAdmin([AVISO_CIERRE, AVISO_INACTIVO])),
    ...rutas,
  })
  return { ...simulado, ...renderApp(ruta) }
}

const tabla = () => screen.findByRole('table')
const region = () =>
  screen.getAllByRole('status').find((elemento) => elemento.classList.contains('admin-cuentas__region'))!
const listados = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.url.startsWith(`${BASE}?`))
const pedidasDelBanner = (solicitudes: SolicitudSimulada[]) =>
  solicitudes.filter((s) => s.url === '/api/v1/catalogo/avisos')

describe('WF-14, gestión de avisos (RF40, DEC-74)', () => {
  it('lista los avisos con mensaje completo en el DOM, tipo, fecha, estado y «Despublicar» solo en los activos', async () => {
    const { solicitudes } = montar()

    const filas = within(await tabla()).getAllByRole('row').slice(1)
    const celdas = (fila: HTMLElement) => within(fila).getAllByRole('cell')
    // Recorte solo visual (CSS): el texto completo está en el DOM.
    expect(celdas(filas[0])[0]).toHaveTextContent(AVISO_CIERRE.mensaje)
    expect(celdas(filas[0])[1]).toHaveTextContent('Cierre')
    expect(celdas(filas[0])[2]).toHaveTextContent('22/10/2026')
    expect(celdas(filas[0])[3]).toHaveTextContent('Activo')
    expect(within(filas[0]).getByRole('button', { name: /^Despublicar/ })).toBeInTheDocument()
    expect(celdas(filas[1])[3]).toHaveTextContent('Inactivo')
    expect(within(filas[1]).queryByRole('button')).not.toBeInTheDocument()
    expect(celdas(filas[1])[4]).toHaveTextContent('Sin acciones: el aviso ya está despublicado.')
    expect(new URL(listados(solicitudes)[0].url, 'http://x').searchParams.get('pagina')).toBe('0')
    expect(listados(solicitudes)[0].cabeceras.Authorization).toBe('Bearer token-admin')
  })

  it('pagina de 10 con la página en la URL (desde 1; la API desde 0)', async () => {
    const { usuario, solicitudes } = montar({
      [LISTADO]: (solicitud) => {
        const pagina = Number(new URL(solicitud.url, 'http://x').searchParams.get('pagina'))
        return respuestaJson(200, paginaAdmin(pagina === 0 ? [AVISO_CIERRE] : [AVISO_INACTIVO], { pagina, totalElementos: 12 }))
      },
    })
    await tabla()
    expect(screen.getByText('Página 1 de 2')).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Página 2' }))

    await waitFor(() => expect(ubicacionActual()).toBe('/admin/avisos?pagina=2'))
    await waitFor(() => expect(screen.getByText('Página 2 de 2')).toBeInTheDocument())
    expect(new URL(listados(solicitudes).at(-1)!.url, 'http://x').searchParams.get('pagina')).toBe('1')
  })

  it('publicar: contador de 300, «Clima» por defecto, 201 avisa, limpia el formulario y actualiza el banner sin recargar', async () => {
    const nuevo = { ...AVISO_CLIMA, id: '6a1e0000000000000000000f', mensaje: 'Marea alta en El Rodadero.', tipo: 'Cierre' }
    let publicado = false
    const { usuario, solicitudes } = montar({
      [`POST ${BASE}`]: () => {
        publicado = true
        return respuestaJson(201, nuevo)
      },
      [BANNER]: () => respuestaJson(200, listaElementos(publicado ? [nuevo, AVISO_CIERRE] : [AVISO_CIERRE])),
    })
    await tabla()

    const mensaje = screen.getByLabelText('Mensaje (obligatorio)')
    const tipo = screen.getByLabelText('Tipo')
    const publicar = screen.getByRole('button', { name: 'Publicar' })
    expect(tipo).toHaveValue('Clima')
    expect(publicar).toHaveAttribute('aria-disabled', 'true')

    await usuario.type(mensaje, ' Marea alta en El Rodadero. ')
    expect(screen.getByText('26 de 300 caracteres')).toBeInTheDocument()
    await usuario.selectOptions(tipo, 'Cierre')
    await usuario.click(publicar)

    await waitFor(() => expect(region()).toHaveTextContent('Se publicó el aviso; ya aparece en el banner.'))
    expect(solicitudes.find((s) => s.metodo === 'POST')!.cuerpo).toEqual({ mensaje: 'Marea alta en El Rodadero.', tipo: 'Cierre' })
    expect(mensaje).toHaveValue('')
    expect(tipo).toHaveValue('Clima')
    await waitFor(() => expect(listados(solicitudes)).toHaveLength(2))
    // El banner común lo muestra sin recargar la página (DEC-72).
    const banner = await screen.findByRole('region', { name: 'Avisos del municipio' })
    await waitFor(() => expect(within(banner).getByText('Marea alta en El Rodadero.')).toBeInTheDocument())
    expect(pedidasDelBanner(solicitudes).length).toBeGreaterThanOrEqual(2)
  })

  it('más de 300 se marca mientras se escribe; un 400 muestra los códigos de mensaje y tipo en sus campos', async () => {
    const { usuario } = montar({
      [`POST ${BASE}`]: () => respuestaProblema(400, 'DATOS_INVALIDOS', { campos: { mensaje: 'REQUERIDO', tipo: 'NO_PERMITIDO' } }),
    })
    await tabla()
    const mensaje = screen.getByLabelText('Mensaje (obligatorio)')

    await usuario.click(mensaje)
    await usuario.paste('a'.repeat(301))
    expect(screen.getByText('301 de 300 caracteres')).toBeInTheDocument()
    expect(mensaje).toHaveAccessibleDescription(/Admite como máximo 300 caracteres\./)
    expect(screen.getByRole('button', { name: 'Publicar' })).toHaveAttribute('aria-disabled', 'true')

    await usuario.clear(mensaje)
    await usuario.type(mensaje, 'x')
    await usuario.click(screen.getByRole('button', { name: 'Publicar' }))

    await waitFor(() => expect(mensaje).toHaveAccessibleDescription(/Este campo es obligatorio\./))
    expect(screen.getByLabelText('Tipo')).toHaveAccessibleDescription('Elige un tipo de la lista.')
  })

  it('despublicar pide confirmación con un extracto del mensaje; el 200 reemplaza la fila y actualiza el banner', async () => {
    const { usuario, solicitudes } = montar({
      [`POST ${BASE}/${AVISO_CIERRE.id}/despublicacion`]: () => respuestaJson(200, { ...AVISO_CIERRE, activo: false }),
    })
    const filas = within(await tabla()).getAllByRole('row').slice(1)
    const avisosAntes = pedidasDelBanner(solicitudes).length

    await usuario.click(within(filas[0]).getByRole('button', { name: /^Despublicar/ }))
    const dialogo = screen.getByRole('dialog', { name: 'Despublicar aviso' })
    // Extracto de 60 puntos de código (el mensaje tiene 72).
    expect(dialogo).toHaveTextContent('«El sendero de la Cascada El Salto está cerrado temporalmente…» deja de verse')
    expect(within(dialogo).getByRole('button', { name: 'Cancelar' })).toHaveFocus()
    await usuario.click(within(dialogo).getByRole('button', { name: 'Despublicar' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    const fila = within(screen.getByRole('table')).getAllByRole('row')[1]
    expect(within(fila).getAllByRole('cell')[3]).toHaveTextContent('Inactivo')
    expect(within(fila).queryByRole('button')).not.toBeInTheDocument()
    expect(region()).toHaveTextContent('Se despublicó el aviso')
    // Se reemplaza en su sitio, sin recargar la lista (DEC-52); el banner sí vuelve a pedir los activos.
    expect(listados(solicitudes)).toHaveLength(1)
    await waitFor(() => expect(pedidasDelBanner(solicitudes).length).toBe(avisosAntes + 1))
  })

  it('un 409 ESTADO_CAMBIADO (ya estaba despublicado) cierra el diálogo, avisa y recarga (DEC-52)', async () => {
    const { usuario, solicitudes } = montar({
      [`POST ${BASE}/${AVISO_CIERRE.id}/despublicacion`]: () =>
        respuestaProblema(409, 'ESTADO_CAMBIADO', { estadoActual: 'Inactivo' }),
    })
    await tabla()
    await usuario.click(screen.getByRole('button', { name: /^Despublicar/ }))
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Despublicar' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(region()).toHaveTextContent('El aviso ya estaba despublicado; se recargó la lista.')
    await waitFor(() => expect(listados(solicitudes)).toHaveLength(2))
  })

  it('un 404 AVISO_NO_ENCONTRADO avisa y recarga', async () => {
    const { usuario, solicitudes } = montar({
      [`POST ${BASE}/${AVISO_CIERRE.id}/despublicacion`]: () => respuestaProblema(404, 'AVISO_NO_ENCONTRADO'),
    })
    await tabla()
    await usuario.click(screen.getByRole('button', { name: /^Despublicar/ }))
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Despublicar' }))

    await waitFor(() => expect(region()).toHaveTextContent('El aviso ya no existe; se recargó la lista.'))
    await waitFor(() => expect(listados(solicitudes)).toHaveLength(2))
  })

  it('un error de red al despublicar se muestra en el diálogo, que sigue abierto', async () => {
    const { usuario } = montar({
      [`POST ${BASE}/${AVISO_CIERRE.id}/despublicacion`]: () => {
        throw new TypeError('Failed to fetch')
      },
    })
    await tabla()
    await usuario.click(screen.getByRole('button', { name: /^Despublicar/ }))
    const dialogo = screen.getByRole('dialog')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Despublicar' }))

    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('No fue posible conectar con el servidor.')
  })
})

describe('Seguridad de la gestión de avisos (RNF06, DEC-51)', () => {
  it.each([USUARIO_TURISTA, USUARIO_PRESTADOR])('$rol ve «Sin permiso» y no se pide el listado', async (usuarioActual) => {
    sessionStorage.setItem('samaria360.token', 'token-otro')
    const { solicitudes } = simularFetch({ [ME]: () => respuestaJson(200, usuarioActual) })
    renderApp(RUTA)

    expect(await screen.findByRole('heading', { level: 1, name: 'Sin permiso' })).toBeInTheDocument()
    expect(listados(solicitudes)).toHaveLength(0)
  })

  it('un 403 ACCESO_DENEGADO del listado muestra «Sin permiso» sin cerrar la sesión', async () => {
    montar({ [LISTADO]: () => respuestaProblema(403, 'ACCESO_DENEGADO') })

    expect(await screen.findByRole('heading', { level: 1, name: 'Sin permiso' })).toBeInTheDocument()
    expect(sessionStorage.getItem('samaria360.token')).toBe('token-admin')
  })

  it('sin sesión lleva al login', async () => {
    simularFetch({})
    renderApp(RUTA)

    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
  })

  it('un 401 SESION_INVALIDADA al publicar cierra la sesión y el login dice por qué (DEC-49, DEC-50)', async () => {
    const { usuario } = montar({ [`POST ${BASE}`]: () => respuestaProblema(401, 'SESION_INVALIDADA') })
    await tabla()
    await usuario.type(screen.getByLabelText('Mensaje (obligatorio)'), 'Aviso')
    await usuario.click(screen.getByRole('button', { name: 'Publicar' }))

    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
    expect(
      await screen.findByText('Tu sesión se cerró porque cambió el estado o los permisos de tu cuenta.'),
    ).toBeInTheDocument()
  })
})
