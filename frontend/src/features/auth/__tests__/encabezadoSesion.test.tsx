import { act, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  USUARIO_TURISTA,
  paginaOferta,
  respuestaJson,
  respuestaProblema,
  simularFetch,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'
import { destinoTrasLogin } from '../destinoTrasLogin'
import { LIMITE_VALIDACION_MS } from '../SesionContext'

const ME = 'GET /api/v1/auth/me'
const LOGIN = 'POST /api/v1/auth/login'
const CATALOGO = { 'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([])) }

const USUARIO_GESTOR = {
  idUsuario: '00000000-0000-4000-8000-000000000003',
  correo: 'gestor@samaria360.test',
  rol: 'Gestor',
  estadoCuenta: 'Activo',
  nombreVisible: 'Luis Gómez',
}

const enlaceInicioSesion = () => screen.queryByRole('link', { name: 'Inicio de sesión' })
const token = () => sessionStorage.getItem('samaria360.token')

/** Promesa que la prueba resuelve cuando quiere (respuesta lenta del servidor). */
function diferida<T>() {
  let resolver!: (valor: T) => void
  const promesa = new Promise<T>((r) => {
    resolver = r
  })
  return { promesa, resolver }
}

afterEach(() => {
  vi.useRealTimers()
})

describe('Encabezado según la sesión', () => {
  it('sin sesión, «Inicio de sesión» aparece de inmediato y no se consulta /me', () => {
    const { fetchSimulado } = simularFetch({ ...CATALOGO })
    renderApp('/')

    expect(enlaceInicioSesion()).toBeInTheDocument()
    expect(fetchSimulado.mock.calls.some(([url]) => String(url).includes('/auth/me'))).toBe(false)
  })

  it('con sesión, el enlace no aparece (tampoco mientras se valida) y sí aparece el avatar', async () => {
    sessionStorage.setItem('samaria360.token', 'token-guardado')
    simularFetch({ [ME]: () => respuestaJson(200, USUARIO_TURISTA), ...CATALOGO })
    renderApp('/')

    // Mientras /me responde: ni el enlace ni el avatar, así no aparece y desaparece.
    expect(enlaceInicioSesion()).not.toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Menú de usuario de Ana Pérez' })).toBeInTheDocument()
    expect(enlaceInicioSesion()).not.toBeInTheDocument()
  })

  // Cerrar sesión lleva a /login, que en la v2 tiene el encabezado reducido (sin navegación): el enlace se comprueba al
  // volver al inicio con el logo, sin recargar.
  it('al cerrar sesión, el enlace vuelve sin recargar', async () => {
    sessionStorage.setItem('samaria360.token', 'token-guardado')
    simularFetch({ [ME]: () => respuestaJson(200, USUARIO_TURISTA), ...CATALOGO })
    const { usuario } = renderApp('/')

    await usuario.click(await screen.findByRole('button', { name: 'Menú de usuario de Ana Pérez' }))
    await usuario.click(screen.getByRole('menuitem', { name: 'Cerrar sesión' }))

    expect(await screen.findByLabelText('Correo electrónico')).toBeInTheDocument()
    expect(ubicacionActual()).toBe('/login')
    expect(screen.queryByRole('button', { name: /Menú de usuario/ })).not.toBeInTheDocument()
    expect(token()).toBeNull()

    await usuario.click(screen.getByRole('link', { name: 'Samaria360, ir al inicio' }))
    expect(await screen.findByRole('link', { name: 'Inicio de sesión' })).toBeInTheDocument()
  })

  // Antes, un 401 de la búsqueda cerraba la sesión (N11): la búsqueda es pública y ahora sale sin token (DEC-47), así
  // que su respuesta no dice nada de la sesión. El cierre por una petición autenticada está en sesionVencimiento.test.tsx.
  it('la búsqueda pública sale sin token y su respuesta no cierra la sesión (N11)', async () => {
    sessionStorage.setItem('samaria360.token', 'token-guardado')
    const busqueda = diferida<Response>()
    const { solicitudes } = simularFetch({
      [ME]: () => respuestaJson(200, USUARIO_TURISTA),
      'GET /api/v1/catalogo/oferta': () => busqueda.promesa,
    })
    renderApp('/explorar')

    await screen.findByRole('button', { name: 'Menú de usuario de Ana Pérez' })
    const oferta = () => solicitudes.filter((s) => s.url.startsWith('/api/v1/catalogo/oferta'))
    await waitFor(() => expect(oferta().length).toBeGreaterThan(0))
    await act(async () => busqueda.resolver(respuestaProblema(401, 'NO_AUTENTICADO')))

    expect(oferta().every((s) => s.cabeceras.Authorization === undefined)).toBe(true)
    expect(screen.getByRole('button', { name: 'Menú de usuario de Ana Pérez' })).toBeInTheDocument()
    expect(screen.queryByText(/Tu sesión expiró\./)).not.toBeInTheDocument()
    expect(token()).toBe('token-guardado')
  })

  it('/me con 401: el enlace aparece y se borra el token', async () => {
    sessionStorage.setItem('samaria360.token', 'token-guardado')
    simularFetch({ [ME]: () => respuestaProblema(401, 'NO_AUTENTICADO'), ...CATALOGO })
    renderApp('/')

    expect(await screen.findByRole('link', { name: 'Inicio de sesión' })).toBeInTheDocument()
    expect(token()).toBeNull()
  })

  it.each([
    ['503 (servicio no disponible)', () => respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE', { servicio: 'ms-auth' })],
    [
      'error de red',
      () => {
        throw new TypeError('Failed to fetch')
      },
    ],
  ])('/me con %s: sesión no confirmada, el enlace aparece y el token se conserva', async (_caso, respuesta) => {
    sessionStorage.setItem('samaria360.token', 'token-guardado')
    simularFetch({ [ME]: respuesta, ...CATALOGO })
    renderApp('/')

    expect(await screen.findByRole('link', { name: 'Inicio de sesión' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Menú de usuario/ })).not.toBeInTheDocument()
    expect(token()).toBe('token-guardado')
  })

  it(`/me sin respuesta: a los ${LIMITE_VALIDACION_MS / 1000} s el enlace aparece y el token se conserva`, async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    sessionStorage.setItem('samaria360.token', 'token-guardado')
    simularFetch({ [ME]: () => new Promise<Response>(() => {}), ...CATALOGO })
    renderApp('/')

    expect(enlaceInicioSesion()).not.toBeInTheDocument()
    await act(async () => {
      await vi.advanceTimersByTimeAsync(LIMITE_VALIDACION_MS)
    })
    expect(enlaceInicioSesion()).toBeInTheDocument()
    expect(token()).toBe('token-guardado')
  })

  it('una respuesta tardía de /me con el token anterior se descarta si hubo un nuevo inicio de sesión', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    sessionStorage.setItem('samaria360.token', 'token-a')
    const meLento = diferida<Response>()
    simularFetch({
      [ME]: ({ cabeceras }) =>
        cabeceras.Authorization === 'Bearer token-a' ? meLento.promesa : respuestaJson(200, USUARIO_GESTOR),
      [LOGIN]: () =>
        respuestaJson(200, { token: 'token-b', tipo: 'Bearer', expiraEn: '2026-10-12T05:00:00Z', idUsuario: USUARIO_GESTOR.idUsuario, rol: 'Gestor' }),
      ...CATALOGO,
    })
    renderApp('/login')

    // /me con el token A no responde: a los 8 s la sesión queda sin confirmar y aparece el formulario.
    await act(async () => {
      await vi.advanceTimersByTimeAsync(LIMITE_VALIDACION_MS)
    })
    fireEvent.change(await screen.findByLabelText('Correo electrónico'), { target: { value: 'gestor@samaria360.test' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'Samaria360*' } })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    expect(await screen.findByRole('button', { name: 'Menú de usuario de Luis Gómez' })).toBeInTheDocument()

    // Llega tarde la respuesta de /me con el token A (otra persona): se descarta.
    await act(async () => meLento.resolver(respuestaJson(200, USUARIO_TURISTA)))
    expect(screen.getByRole('button', { name: 'Menú de usuario de Luis Gómez' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Menú de usuario de Ana Pérez' })).not.toBeInTheDocument()
    expect(token()).toBe('token-b')
  })
})

describe('/login con la sesión iniciada', () => {
  it('redirige al inicio sin mostrar el formulario', async () => {
    sessionStorage.setItem('samaria360.token', 'token-guardado')
    simularFetch({ [ME]: () => respuestaJson(200, USUARIO_TURISTA), ...CATALOGO })
    renderApp('/login')

    expect(screen.queryByLabelText('Correo electrónico')).not.toBeInTheDocument()
    await waitFor(() => expect(ubicacionActual()).toBe('/'))
    expect(screen.queryByLabelText('Correo electrónico')).not.toBeInTheDocument()
  })

  it('redirige a la ruta de origen, con su consulta y su ancla', async () => {
    sessionStorage.setItem('samaria360.token', 'token-guardado')
    simularFetch({ [ME]: () => respuestaJson(200, USUARIO_TURISTA), ...CATALOGO })
    renderApp({ pathname: '/login', state: { desde: '/explorar?texto=kayak#resultados' } })

    await waitFor(() => expect(ubicacionActual()).toBe('/explorar?texto=kayak'))
  })

  it.each(['//sitio-externo.com', '/\\sitio-externo.com', 'https://sitio-externo.com', '/login'])(
    'con una ruta de origen no válida (%s), redirige al inicio',
    async (desde) => {
      sessionStorage.setItem('samaria360.token', 'token-guardado')
      simularFetch({ [ME]: () => respuestaJson(200, USUARIO_TURISTA), ...CATALOGO })
      renderApp({ pathname: '/login', state: { desde } })

      await waitFor(() => expect(ubicacionActual()).toBe('/'))
    },
  )
})

describe('destinoTrasLogin', () => {
  it.each([
    ['/explorar?texto=kayak', '/explorar?texto=kayak'],
    ['/', '/'],
    ['//sitio-externo.com', '/'],
    ['/\\sitio-externo.com', '/'],
    ['https://sitio-externo.com', '/'],
    ['/login', '/'],
    ['/login?x=1', '/'],
    ['/loginx', '/loginx'],
    [undefined, '/'],
    [42, '/'],
  ])('%s → %s', (desde, esperado) => {
    expect(destinoTrasLogin(desde)).toBe(esperado)
  })
})
