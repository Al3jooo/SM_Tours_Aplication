import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  USUARIO_ADMIN,
  USUARIO_GESTOR,
  USUARIO_PRESTADOR,
  USUARIO_TURISTA,
  paginaServicios,
  respuestaJson,
  servicioCola,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

/** WF-19 «Servicios por aprobar»: cola (CU «Consultar listado pendiente», RF21; DEC-174, DEC-175). */

const ME = 'GET /api/v1/auth/me'
const BASE = '/api/v1/catalogo/admin/servicios'
const COLA = `GET ${BASE}`
const LENTO = { timeout: 15_000 }

type Manejador = (solicitud: SolicitudSimulada) => Response | Promise<Response>

const consultasCola = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.url.startsWith(`${BASE}?`))
const parametros = (solicitud: SolicitudSimulada) => new URL(solicitud.url, 'http://localhost').searchParams

const KAYAKS = servicioCola({
  id: '6a3000000000000000000002',
  nombreComercial: 'Kayaks Taganga',
  categoria: 'AlquilerEquipo',
  rnt: 'RNT-00002',
  prestadorActivo: false,
  fechaRegistro: '2026-10-22T15:00:00Z',
})

function montar(ruta: string, rutas: Record<string, Manejador> = {}) {
  sessionStorage.setItem('samaria360.token', 'token-admin')
  const simulado = simularFetch({
    [ME]: () => respuestaJson(200, USUARIO_ADMIN),
    [COLA]: () => respuestaJson(200, paginaServicios([servicioCola(), KAYAKS])),
    ...rutas,
  })
  return { ...simulado, ...renderApp(ruta) }
}

describe('Acceso a WF-19 (RNF06, DEC-174)', () => {
  it.each([
    ['Turista', USUARIO_TURISTA, '/admin/servicios'],
    ['Prestador', USUARIO_PRESTADOR, '/admin/servicios'],
    ['Gestor', USUARIO_GESTOR, '/admin/servicios'],
    ['Turista', USUARIO_TURISTA, '/admin/servicios/6a3000000000000000000001'],
    ['Prestador', USUARIO_PRESTADOR, '/admin/servicios/6a3000000000000000000001'],
    ['Gestor', USUARIO_GESTOR, '/admin/servicios/6a3000000000000000000001'],
  ])('un %s en %s ve «Sin permiso» y no se llama a la API', async (_rol, usuario, ruta) => {
    sessionStorage.setItem('samaria360.token', 'token-otro')
    const { solicitudes } = simularFetch({ [ME]: () => respuestaJson(200, usuario) })
    renderApp(ruta)

    expect(await screen.findByRole('heading', { level: 1, name: 'Sin permiso' }, LENTO)).toBeInTheDocument()
    expect(solicitudes.some((s) => s.url.startsWith(BASE))).toBe(false)
  })

  it.each(['/admin/servicios', '/admin/servicios/6a3000000000000000000001'])('sin sesión, %s lleva al login', async (ruta) => {
    renderApp(ruta)
    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
  })

  it('el menú del Administrador lleva a «Aprobar servicios»', async () => {
    const { usuario } = montar('/admin/servicios')
    await usuario.click(await screen.findByRole('button', { name: 'Menú de usuario de Laura Díaz' }, LENTO))

    expect(within(screen.getByRole('menu')).getByRole('menuitem', { name: 'Aprobar servicios' })).toHaveAttribute(
      'href',
      '/admin/servicios',
    )
  })
})

describe('Cola de revisión (DEC-175)', () => {
  it('por defecto pide los Pendientes de la página 0 y muestra las columnas de WF-19', async () => {
    const { solicitudes } = montar('/admin/servicios')

    const tabla = await screen.findByRole('table', {}, LENTO)
    const [consulta] = consultasCola(solicitudes)
    expect(parametros(consulta).get('estado')).toBe('Pendiente')
    expect(parametros(consulta).get('pagina')).toBe('0')
    expect(screen.getByRole('heading', { level: 1, name: 'Servicios por aprobar' })).toBeInTheDocument()

    expect(within(tabla).getAllByRole('columnheader').map((celda) => celda.textContent)).toEqual([
      'Nombre',
      'Categoría',
      'RNT del prestador',
      'Prestador activo',
      'Registrado',
      'Acción',
    ])
    const filaKayaks = within(tabla).getByText('Kayaks Taganga').closest('tr') as HTMLElement
    expect(within(filaKayaks).getByText('RNT-00002')).toBeInTheDocument()
    expect(within(filaKayaks).getByText('No (quedará oculto aunque se apruebe)')).toBeInTheDocument()
    expect(within(filaKayaks).getByText('22/10/2026')).toBeInTheDocument()
    expect(within(filaKayaks).getByRole('link', { name: 'Revisar Kayaks Taganga' })).toHaveAttribute(
      'href',
      '/admin/servicios/6a3000000000000000000002',
    )
    const filaTour = within(tabla).getByText('Tour de Café en Minca').closest('tr') as HTMLElement
    expect(within(filaTour).getByText('Sí')).toBeInTheDocument()

    // Filtro sin «Todos», con los textos de B1 (N21).
    const filtro = screen.getByLabelText('Estado')
    expect(filtro).toHaveValue('Pendiente')
    expect(within(filtro).getAllByRole('option').map((opcion) => opcion.textContent)).toEqual([
      'Pendiente de aprobación',
      'Publicado',
      'Rechazado',
    ])
    expect(screen.getByRole('heading', { level: 2, name: '2 servicios' })).toBeInTheDocument()
  })

  it('el estado y la página van en la URL (desde 1); la API recibe la página desde 0', async () => {
    const { usuario, solicitudes } = montar('/admin/servicios?estado=Rechazado&pagina=2', {
      [COLA]: (solicitud) =>
        respuestaJson(200, paginaServicios([servicioCola({ estadoPublicacion: 'Rechazado' })], Number(parametros(solicitud).get('pagina')), 25)),
    })

    await screen.findByRole('table', {}, LENTO)
    expect(parametros(consultasCola(solicitudes)[0]).get('estado')).toBe('Rechazado')
    expect(parametros(consultasCola(solicitudes)[0]).get('pagina')).toBe('1')
    expect(screen.getByLabelText('Estado')).toHaveValue('Rechazado')

    await usuario.click(screen.getByRole('button', { name: 'Página 3' }))
    await waitFor(() => expect(ubicacionActual()).toBe('/admin/servicios?estado=Rechazado&pagina=3'))
    await waitFor(() => expect(parametros(consultasCola(solicitudes).at(-1)!).get('pagina')).toBe('2'))

    // Cambiar el filtro vuelve a la página 1; «Pendiente» es el valor por defecto y no se escribe.
    await usuario.selectOptions(screen.getByLabelText('Estado'), 'Pendiente')
    await waitFor(() => expect(ubicacionActual()).toBe('/admin/servicios'))
    await waitFor(() => expect(parametros(consultasCola(solicitudes).at(-1)!).get('estado')).toBe('Pendiente'))
  })

  it('un estado que no existe se normaliza al de por defecto', async () => {
    const { solicitudes } = montar('/admin/servicios?estado=Todos&pagina=0')

    await screen.findByRole('table', {}, LENTO)
    await waitFor(() => expect(ubicacionActual()).toBe('/admin/servicios'))
    expect(consultasCola(solicitudes).every((s) => parametros(s).get('estado') === 'Pendiente')).toBe(true)
  })

  it('una página fuera de rango lleva a la última', async () => {
    montar('/admin/servicios?pagina=4', {
      [COLA]: (solicitud) =>
        respuestaJson(
          200,
          parametros(solicitud).get('pagina') === '3'
            ? paginaServicios([], 3, 12)
            : paginaServicios([servicioCola()], Number(parametros(solicitud).get('pagina')), 12),
        ),
    })

    await waitFor(() => expect(ubicacionActual()).toBe('/admin/servicios?pagina=2'), LENTO)
    expect(await screen.findByRole('table')).toBeInTheDocument()
  })

  it.each([
    ['Pendiente', '/admin/servicios', 'No hay servicios pendientes de aprobación.'],
    ['Publicado', '/admin/servicios?estado=Publicado', 'No hay servicios publicados.'],
  ])('cola vacía (%s): el mensaje va en la región de estado, sin tabla', async (_estado, ruta, mensaje) => {
    montar(ruta, { [COLA]: () => respuestaJson(200, paginaServicios([])) })

    const texto = await screen.findByText(mensaje, {}, LENTO)
    expect(texto.closest('[aria-live="polite"]')).not.toBeNull()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('un error de red muestra «Reintentar», que vuelve a pedir la cola', async () => {
    let intentos = 0
    const { usuario } = montar('/admin/servicios', {
      [COLA]: () => {
        intentos += 1
        if (intentos === 1) throw new TypeError('Failed to fetch')
        return respuestaJson(200, paginaServicios([servicioCola()]))
      },
    })

    const alerta = await screen.findByRole('alert', {}, LENTO)
    expect(alerta).toHaveTextContent('No fue posible conectar con el servidor. Revisa tu conexión.')
    await usuario.click(within(alerta).getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByRole('table')).toBeInTheDocument()
  })
})
