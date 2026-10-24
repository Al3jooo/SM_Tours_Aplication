import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  BUSQUEDA_CERCA,
  BUSQUEDA_PLAYA,
  HOTEL,
  USUARIO_TURISTA,
  listaElementos,
  paginaOferta,
  respuestaJson,
  respuestaProblema,
  simularFetch,
  ultimaConsultaCatalogo,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { simularGeolocalizacion } from '../../../test/geolocalizacionSimulada'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

const ME = 'GET /api/v1/auth/me'
const HISTORIAL = '/api/v1/catalogo/historial'
const OFERTA = 'GET /api/v1/catalogo/oferta'
const EL_RODADERO = { latitud: 11.2047, longitud: -74.2272 }

/** La primera carga de una página diferida puede pasar de 5 s con toda la suite en paralelo (ver test/setup.ts). */
const LENTO = { timeout: 15_000 }

const posts = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.metodo === 'POST' && s.url === HISTORIAL)
const consultasHistorial = (solicitudes: SolicitudSimulada[]) =>
  solicitudes.filter((s) => s.metodo === 'GET' && s.url === HISTORIAL)

function comoTurista(rutas: Parameters<typeof simularFetch>[0] = {}) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  return simularFetch({
    [ME]: () => respuestaJson(200, USUARIO_TURISTA),
    [OFERTA]: () => respuestaJson(200, paginaOferta([HOTEL], { totalElementos: 15 })),
    [`POST ${HISTORIAL}`]: () => new Response(null, { status: 204 }),
    [`GET ${HISTORIAL}`]: () => respuestaJson(200, listaElementos([])),
    ...rutas,
  })
}

async function cargarResultados() {
  await screen.findByRole('heading', { level: 2, name: /15 lugares encontrados/ }, LENTO)
}

describe('Historial de búsquedas: registro (RF60, DEC-95)', () => {
  it('una búsqueda del turista envía POST con los criterios y usarUbicacion, sin lat, lng, pagina ni tamano', async () => {
    const { solicitudes } = comoTurista()
    renderApp('/explorar?texto=playa&categoria=Alojamiento&serviciosIncluidos=WiFi&precioMax=200000&pagina=2')
    await cargarResultados()

    await waitFor(() => expect(posts(solicitudes)).toHaveLength(1))
    const [post] = posts(solicitudes)
    expect(post.cabeceras.Authorization).toBe('Bearer token-guardado')
    expect(post.cuerpo).toEqual({
      texto: 'playa',
      categoria: 'Alojamiento',
      serviciosIncluidos: ['WiFi'],
      precioMax: 200000,
      usarUbicacion: false,
    })
    for (const campo of ['lat', 'lng', 'pagina', 'tamano']) expect(post.cuerpo).not.toHaveProperty(campo)
    // La búsqueda pública sigue sin token (DEC-47).
    const oferta = solicitudes.filter((s) => s.url.startsWith('/api/v1/catalogo/oferta'))
    expect(oferta.every((s) => s.cabeceras.Authorization === undefined)).toBe(true)
  })

  it('paginar no envía POST; aplicar otro filtro sí', async () => {
    const { solicitudes } = comoTurista()
    const { usuario } = renderApp('/explorar?texto=playa')
    await cargarResultados()
    await waitFor(() => expect(posts(solicitudes)).toHaveLength(1))

    await usuario.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsultaCatalogo(solicitudes)).toContain('pagina=1'))
    await cargarResultados()
    expect(posts(solicitudes)).toHaveLength(1)

    await usuario.click(screen.getByRole('checkbox', { name: 'Movilidad reducida' }))
    await usuario.click(screen.getByRole('button', { name: 'Aplicar filtros' }))
    await waitFor(() => expect(posts(solicitudes)).toHaveLength(2))
    expect(posts(solicitudes)[1].cuerpo).toEqual({ texto: 'playa', accesibilidad: ['MovilidadReducida'], usarUbicacion: false })
  })

  it('la misma búsqueda en la página (re-render sin cambio de clave) envía un solo POST', async () => {
    const { solicitudes } = comoTurista()
    const { usuario } = renderApp('/explorar?texto=playa')
    await cargarResultados()
    await waitFor(() => expect(posts(solicitudes)).toHaveLength(1))

    await usuario.click(screen.getByRole('button', { name: 'Aplicar filtros' }))
    await usuario.click(screen.getByRole('button', { name: 'Mapa' }))
    await usuario.click(screen.getByRole('button', { name: 'Lista' }))
    await cargarResultados()

    expect(posts(solicitudes)).toHaveLength(1)
  })

  it('la carga inicial de /explorar sin criterios no envía POST', async () => {
    const { solicitudes } = comoTurista()
    renderApp('/explorar')
    await cargarResultados()
    await waitFor(() => expect(consultasHistorial(solicitudes).length).toBeGreaterThan(0))

    expect(posts(solicitudes)).toHaveLength(0)
  })

  it('si /oferta falla, no se envía POST', async () => {
    const { solicitudes } = comoTurista({ [OFERTA]: () => respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE') })
    renderApp('/explorar?texto=playa')

    expect(await screen.findByText(/El catálogo no está disponible/, {}, LENTO)).toBeInTheDocument()
    expect(posts(solicitudes)).toHaveLength(0)
  })

  it('un error del POST no afecta los resultados ni se muestra', async () => {
    const { solicitudes } = comoTurista({ [`POST ${HISTORIAL}`]: () => respuestaProblema(500, 'ERROR_INTERNO') })
    renderApp('/explorar?texto=playa')
    await cargarResultados()
    await waitFor(() => expect(posts(solicitudes)).toHaveLength(1))

    expect(screen.getByRole('heading', { name: 'Hotel Casa Tayrona' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('un visitante no envía POST ni pide el historial, y no ve «Últimas búsquedas»', async () => {
    const { solicitudes } = simularFetch({ [OFERTA]: () => respuestaJson(200, paginaOferta([HOTEL], { totalElementos: 15 })) })
    renderApp('/explorar?texto=playa')
    await cargarResultados()

    expect(solicitudes.some((s) => s.url.startsWith(HISTORIAL))).toBe(false)
    expect(screen.queryByRole('region', { name: 'Últimas búsquedas' })).not.toBeInTheDocument()
  })

  it('un 401 SESION_INVALIDADA del historial cierra la sesión con su motivo (DEC-50)', async () => {
    comoTurista({ [`GET ${HISTORIAL}`]: () => respuestaProblema(401, 'SESION_INVALIDADA') })
    renderApp('/explorar')

    expect(await screen.findByText(/cambió el estado o los permisos de tu cuenta/, {}, LENTO)).toBeInTheDocument()
    expect(sessionStorage.getItem('samaria360.token')).toBeNull()
  })
})

describe('«Últimas búsquedas» en WF-05 (RF60, DEC-96)', () => {
  it('muestra hasta 10, la más reciente primero, con su texto; después de registrar se vuelve a pedir', async () => {
    const once = Array.from({ length: 11 }, (_, i) => ({ ...BUSQUEDA_PLAYA, id: `b-${i}`, criterios: { ...BUSQUEDA_PLAYA.criterios, texto: `t${i}` } }))
    const { solicitudes } = comoTurista({
      [`GET ${HISTORIAL}`]: () => respuestaJson(200, listaElementos([BUSQUEDA_PLAYA, BUSQUEDA_CERCA, ...once])),
    })
    renderApp('/explorar?texto=kayak')

    const region = await screen.findByRole('region', { name: 'Últimas búsquedas' }, LENTO)
    const enlaces = within(region).getAllByRole('link')
    expect(enlaces).toHaveLength(10)
    expect(enlaces[0]).toHaveTextContent(/^“playa” · Alojamiento · WiFi · Hasta \$\s200\.000$/)
    expect(enlaces[1]).toHaveTextContent('Cerca de mí · 5 km')
    expect(enlaces[0]).toHaveAccessibleDescription('Búsqueda del 23/10/2026.')
    expect(enlaces[1]).toHaveAccessibleDescription('Búsqueda del 22/10/2026. Volverá a pedir tu ubicación.')
    expect(consultasHistorial(solicitudes)[0].cabeceras.Authorization).toBe('Bearer token-guardado')

    await waitFor(() => expect(posts(solicitudes)).toHaveLength(1))
    await waitFor(() => expect(consultasHistorial(solicitudes)).toHaveLength(2))
  })

  it('un clic repite la búsqueda: la URL lleva los criterios', async () => {
    const { solicitudes } = comoTurista({
      [`GET ${HISTORIAL}`]: () => respuestaJson(200, listaElementos([BUSQUEDA_PLAYA])),
    })
    const { usuario } = renderApp('/explorar')
    const region = await screen.findByRole('region', { name: 'Últimas búsquedas' }, LENTO)

    await usuario.click(within(region).getByRole('link'))

    await waitFor(() =>
      expect(ubicacionActual()).toBe('/explorar?texto=playa&categoria=Alojamiento&serviciosIncluidos=WiFi&precioMax=200000'),
    )
    await waitFor(() => expect(ultimaConsultaCatalogo(solicitudes)).toContain('texto=playa'))
    await waitFor(() => expect(posts(solicitudes)).toHaveLength(1))
  })

  it('con usarUbicacion vuelve a pedir la ubicación y la búsqueda la usa (sin guardarla)', async () => {
    const getCurrentPosition = simularGeolocalizacion(EL_RODADERO)
    const { solicitudes } = comoTurista({
      [`GET ${HISTORIAL}`]: () => respuestaJson(200, listaElementos([BUSQUEDA_CERCA])),
    })
    const { usuario } = renderApp('/explorar')
    const region = await screen.findByRole('region', { name: 'Últimas búsquedas' }, LENTO)

    await usuario.click(within(region).getByRole('link', { name: 'Cerca de mí · 5 km' }))

    await waitFor(() => expect(getCurrentPosition).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(ultimaConsultaCatalogo(solicitudes)).toContain('distanciaKm=5'))
    expect(ultimaConsultaCatalogo(solicitudes)).toContain('lat=11.2047')
    expect(ubicacionActual()).toBe('/explorar?distanciaKm=5&orden=Distancia')
    await waitFor(() => expect(posts(solicitudes)).toHaveLength(1))
    expect(posts(solicitudes)[0].cuerpo).toEqual({ distanciaKm: 5, orden: 'Distancia', usarUbicacion: true })
  })

  it('si se niega la ubicación, la búsqueda corre sin distancia y se avisa una sola vez', async () => {
    simularGeolocalizacion({ codigo: 1 })
    const { solicitudes } = comoTurista({
      [`GET ${HISTORIAL}`]: () => respuestaJson(200, listaElementos([BUSQUEDA_CERCA])),
    })
    const { usuario } = renderApp('/explorar')
    const region = await screen.findByRole('region', { name: 'Últimas búsquedas' }, LENTO)

    await usuario.click(within(region).getByRole('link', { name: 'Cerca de mí · 5 km' }))

    const aviso =
      'No compartiste tu ubicación: la búsqueda se repitió sin la distancia ni el orden por cercanía.'
    expect(await screen.findByText(aviso)).toHaveAttribute('role', 'status')
    expect(screen.getAllByText(aviso)).toHaveLength(1)
    expect(screen.queryByText(/No diste permiso para usar tu ubicación/)).not.toBeInTheDocument()
    await cargarResultados()
    expect(ultimaConsultaCatalogo(solicitudes)).not.toMatch(/distanciaKm|lat=|orden=Distancia/)
    // Sin ubicación y sin otros criterios, no hay nada que guardar.
    expect(posts(solicitudes)).toHaveLength(0)
  })
})
