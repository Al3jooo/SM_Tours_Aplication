import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  CASCADA_CERRADA,
  DETALLE_HOTEL,
  FAVORITO_CASCADA,
  FAVORITO_HOTEL,
  HOTEL,
  SESION_TURISTA,
  USUARIO_ADMIN,
  USUARIO_PRESTADOR,
  USUARIO_TURISTA,
  listaElementos,
  paginaOferta,
  respuestaJson,
  respuestaProblema,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { mapaVivo } from '../../../test/leafletSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

const ME = 'GET /api/v1/auth/me'
const FAVORITOS = '/api/v1/catalogo/favoritos'
const OFERTA = { 'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([HOTEL, CASCADA_CERRADA])) }
const DETALLE = { 'GET /api/v1/catalogo/servicios/s-hotel': () => respuestaJson(200, DETALLE_HOTEL) }

/** La primera carga de una página diferida puede pasar de 5 s con toda la suite en paralelo (ver test/setup.ts). */
const LENTO = { timeout: 15_000 }
const corazon = (nombre: string) => screen.findByRole('button', { name: `Guardar en favoritos: ${nombre}` }, LENTO)
const listas = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.metodo === 'GET' && s.url === FAVORITOS)
const delTurista = (solicitudes: SolicitudSimulada[]) =>
  solicitudes.filter((s) => s.url.startsWith(FAVORITOS) || s.url.startsWith('/api/v1/catalogo/historial'))

function conSesion(usuario: object, rutas: Parameters<typeof simularFetch>[0] = {}) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  return simularFetch({ [ME]: () => respuestaJson(200, usuario), ...OFERTA, ...DETALLE, ...rutas })
}

const marcador = (titulo: string) => {
  const encontrado = mapaVivo()
    .marcadores()
    .find((m) => m.opciones.title === titulo)
  if (!encontrado) throw new Error(`Sin marcador «${titulo}»`)
  return encontrado
}

async function popupDe(titulo: string) {
  await waitFor(() => marcador(titulo), LENTO)
  return marcador(titulo).abrirPopup()
}

describe('Corazón de favoritos en WF-05 y WF-06 (RF54, DEC-93)', () => {
  it('refleja GET /favoritos (con token) y la búsqueda sigue sin token (DEC-47)', async () => {
    const { solicitudes } = conSesion(USUARIO_TURISTA, {
      [`GET ${FAVORITOS}`]: () => respuestaJson(200, listaElementos([FAVORITO_CASCADA])),
    })
    renderApp('/explorar')

    await waitFor(async () => expect(await corazon('Cascada El Salto')).toHaveAttribute('aria-pressed', 'true'), LENTO)
    expect(await corazon('Hotel Casa Tayrona')).toHaveAttribute('aria-pressed', 'false')
    expect(listas(solicitudes)).toHaveLength(1)
    expect(listas(solicitudes)[0].cabeceras.Authorization).toBe('Bearer token-guardado')
    const oferta = solicitudes.filter((s) => s.url.startsWith('/api/v1/catalogo/oferta'))
    expect(oferta.every((s) => s.cabeceras.Authorization === undefined)).toBe(true)
  })

  it('marcar envía PUT con el tipo correcto y desmarcar, DELETE; aria-pressed y el anuncio cambian', async () => {
    const { solicitudes } = conSesion(USUARIO_TURISTA, {
      [`GET ${FAVORITOS}`]: () => respuestaJson(200, listaElementos([FAVORITO_CASCADA])),
      [`PUT ${FAVORITOS}/Servicio/s-hotel`]: () => respuestaJson(201, FAVORITO_HOTEL),
      [`DELETE ${FAVORITOS}/Atractivo/a-cascada`]: () => new Response(null, { status: 204 }),
    })
    const { usuario } = renderApp('/explorar')
    const hotel = await corazon('Hotel Casa Tayrona')
    await waitFor(() => expect(hotel).not.toHaveAttribute('aria-disabled'), LENTO)

    await usuario.click(hotel)
    expect(hotel).toHaveAttribute('aria-pressed', 'true')
    expect(await screen.findByText('Guardado en favoritos.')).toBeInTheDocument()

    await usuario.click(await corazon('Cascada El Salto'))
    expect(await screen.findByText('Quitado de favoritos.')).toBeInTheDocument()
    expect(await corazon('Cascada El Salto')).toHaveAttribute('aria-pressed', 'false')

    const cambios = solicitudes.filter((s) => s.metodo === 'PUT' || s.metodo === 'DELETE')
    expect(cambios.map((s) => `${s.metodo} ${s.url}`)).toEqual([
      `PUT ${FAVORITOS}/Servicio/s-hotel`,
      `DELETE ${FAVORITOS}/Atractivo/a-cascada`,
    ])
    expect(cambios.every((s) => s.cabeceras.Authorization === 'Bearer token-guardado')).toBe(true)
    // Un solo anuncio, en una sola región: la de ubicación no lo repite.
    expect(screen.getAllByText('Quitado de favoritos.')).toHaveLength(1)
  })

  it('un atractivo se marca con tipo Atractivo', async () => {
    const { solicitudes } = conSesion(USUARIO_TURISTA, {
      [`PUT ${FAVORITOS}/Atractivo/a-cascada`]: () => respuestaJson(201, FAVORITO_CASCADA),
    })
    const { usuario } = renderApp('/explorar')
    const cascada = await corazon('Cascada El Salto')
    await waitFor(() => expect(cascada).not.toHaveAttribute('aria-disabled'), LENTO)

    await usuario.click(cascada)

    await waitFor(() => expect(solicitudes.some((s) => s.metodo === 'PUT' && s.url === `${FAVORITOS}/Atractivo/a-cascada`)).toBe(true))
    expect(cascada).toHaveAttribute('aria-pressed', 'true')
  })

  it('409 LIMITE_FAVORITOS avisa y el corazón vuelve a su estado', async () => {
    conSesion(USUARIO_TURISTA, {
      [`PUT ${FAVORITOS}/Servicio/s-hotel`]: () => respuestaProblema(409, 'LIMITE_FAVORITOS'),
    })
    const { usuario } = renderApp('/explorar')
    const hotel = await corazon('Hotel Casa Tayrona')
    await waitFor(() => expect(hotel).not.toHaveAttribute('aria-disabled'), LENTO)

    await usuario.click(hotel)

    expect(
      await screen.findByText('Llegaste al límite de 100 favoritos. Quita alguno en «Mis favoritos».'),
    ).toBeInTheDocument()
    expect(hotel).toHaveAttribute('aria-pressed', 'false')
  })

  it('WF-06: el corazón está activo; un 404 avisa «Este servicio ya no está disponible» y se revierte', async () => {
    const { solicitudes } = conSesion(USUARIO_TURISTA, {
      [`PUT ${FAVORITOS}/Servicio/s-hotel`]: () => respuestaProblema(404, 'OFERTA_NO_DISPONIBLE'),
    })
    const { usuario } = renderApp('/servicios/s-hotel')
    const hotel = await corazon('Hotel Casa Tayrona')
    await waitFor(() => expect(hotel).not.toHaveAttribute('aria-disabled'), LENTO)
    expect(hotel).toHaveTextContent('Guardar en favoritos')

    await usuario.click(hotel)

    expect(await screen.findByText('Este servicio ya no está disponible.')).toBeInTheDocument()
    expect(hotel).toHaveAttribute('aria-pressed', 'false')
    expect(solicitudes.find((s) => s.url.startsWith('/api/v1/catalogo/servicios'))?.cabeceras.Authorization).toBeUndefined()
    // «Compartir» de la ficha sigue deshabilitado (DEC-90).
    expect(screen.getByRole('button', { name: /^Compartir/ })).toHaveAttribute('aria-disabled', 'true')
  })

  it('un 401 SESION_INVALIDADA cierra la sesión con su motivo y limpia los favoritos', async () => {
    conSesion(USUARIO_TURISTA, {
      [`GET ${FAVORITOS}`]: () => respuestaJson(200, listaElementos([FAVORITO_CASCADA])),
      [`PUT ${FAVORITOS}/Servicio/s-hotel`]: () => respuestaProblema(401, 'SESION_INVALIDADA'),
    })
    const { usuario } = renderApp('/explorar')
    const hotel = await corazon('Hotel Casa Tayrona')
    await waitFor(() => expect(hotel).not.toHaveAttribute('aria-disabled'), LENTO)

    await usuario.click(hotel)

    expect(await screen.findByText(/cambió el estado o los permisos de tu cuenta/)).toBeInTheDocument()
    expect(sessionStorage.getItem('samaria360.token')).toBeNull()
    // Sin sesión, el corazón ya no tiene estado: lleva al login.
    await waitFor(async () => expect(await corazon('Cascada El Salto')).not.toHaveAttribute('aria-pressed'), LENTO)
  })

  it('al cerrar sesión se limpia el estado: otra sesión vuelve a pedir la lista', async () => {
    let listados = 0
    const { solicitudes } = conSesion(USUARIO_TURISTA, {
      [`GET ${FAVORITOS}`]: () => {
        listados += 1
        return respuestaJson(200, listaElementos(listados === 1 ? [FAVORITO_HOTEL] : []))
      },
      'POST /api/v1/auth/login': () => respuestaJson(200, SESION_TURISTA),
    })
    const { usuario } = renderApp('/explorar')
    await waitFor(async () => expect(await corazon('Hotel Casa Tayrona')).toHaveAttribute('aria-pressed', 'true'), LENTO)

    await usuario.click(screen.getByRole('button', { name: 'Menú de usuario de Ana Pérez' }))
    await usuario.click(screen.getByRole('menuitem', { name: 'Cerrar sesión' }))
    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
    await usuario.type(screen.getByLabelText('Correo electrónico'), 'turista@samaria360.test')
    await usuario.type(screen.getByLabelText('Contraseña'), 'Samaria360*')
    await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    await screen.findByRole('button', { name: 'Menú de usuario de Ana Pérez' })
    await usuario.click(screen.getByRole('link', { name: 'Búsqueda' }))

    await waitFor(async () => expect(await corazon('Hotel Casa Tayrona')).toHaveAttribute('aria-pressed', 'false'), LENTO)
    expect(listas(solicitudes)).toHaveLength(2)
  })

  it('un error de GET /favoritos no queda fijo: el siguiente montaje de un corazón lo vuelve a intentar', async () => {
    let intentos = 0
    const { solicitudes } = conSesion(USUARIO_TURISTA, {
      [`GET ${FAVORITOS}`]: () => {
        intentos += 1
        return intentos === 1 ? respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE') : respuestaJson(200, listaElementos([FAVORITO_HOTEL]))
      },
    })
    const { usuario } = renderApp('/explorar')
    const hotel = await corazon('Hotel Casa Tayrona')
    await waitFor(() => expect(hotel).toHaveAccessibleDescription('No pudimos cargar tus favoritos.'))
    expect(hotel).toHaveAttribute('aria-disabled', 'true')

    // Pulsarlo sin la lista no hace nada.
    await usuario.click(hotel)
    expect(solicitudes.some((s) => s.metodo === 'PUT')).toBe(false)

    // Otra pantalla con corazón (WF-06): se vuelve a pedir.
    await usuario.click(within(screen.getByRole('article', { name: 'Hotel Casa Tayrona' })).getByRole('link', { name: 'Ver detalle' }))
    await screen.findByRole('heading', { level: 1, name: 'Hotel Casa Tayrona' })
    await waitFor(async () => expect(await corazon('Hotel Casa Tayrona')).toHaveAttribute('aria-pressed', 'true'), LENTO)
    expect(listas(solicitudes)).toHaveLength(2)
  })

  it('sin sesión el corazón se ve y lleva al login; al iniciar sesión vuelve a la búsqueda sin marcar nada', async () => {
    const { solicitudes } = simularFetch({
      ...OFERTA,
      'POST /api/v1/auth/login': () => respuestaJson(200, SESION_TURISTA),
      [ME]: () => respuestaJson(200, USUARIO_TURISTA),
    })
    const { usuario } = renderApp('/explorar?texto=hotel')
    const hotel = await corazon('Hotel Casa Tayrona')
    expect(hotel).not.toHaveAttribute('aria-pressed')
    expect(hotel).toHaveAccessibleDescription('Inicia sesión como turista para guardar favoritos.')

    await usuario.click(hotel)
    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
    expect(listas(solicitudes)).toHaveLength(0)

    await usuario.type(screen.getByLabelText('Correo electrónico'), 'turista@samaria360.test')
    await usuario.type(screen.getByLabelText('Contraseña'), 'Samaria360*')
    await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }))

    await waitFor(() => expect(ubicacionActual()).toBe('/explorar?texto=hotel'), LENTO)
    await waitFor(async () => expect(await corazon('Hotel Casa Tayrona')).toHaveAttribute('aria-pressed', 'false'), LENTO)
    expect(solicitudes.some((s) => s.metodo === 'PUT')).toBe(false)
  })

  it.each([
    ['Prestador', USUARIO_PRESTADOR, '/explorar'],
    ['Prestador', USUARIO_PRESTADOR, '/servicios/s-hotel'],
    ['Administrador', USUARIO_ADMIN, '/explorar'],
    ['Administrador', USUARIO_ADMIN, '/servicios/s-hotel'],
  ])('con la sesión de un %s en %s no hay corazón ni llamadas a favoritos o historial', async (_rol, usuarioActual, ruta) => {
    const { solicitudes } = conSesion(usuarioActual)
    renderApp(ruta)

    await screen.findByRole('button', { name: `Menú de usuario de ${usuarioActual.nombreVisible}` })
    await screen.findAllByRole('heading', { name: 'Hotel Casa Tayrona' }, LENTO)
    expect(screen.queryByRole('button', { name: /^Guardar en favoritos/ })).not.toBeInTheDocument()
    if (ruta === '/explorar') {
      await waitFor(() => marcador('Hotel Casa Tayrona'), LENTO)
      expect(marcador('Hotel Casa Tayrona').abrirPopup().querySelector('.popup-oferta__favorito')).toBeNull()
    }
    expect(delTurista(solicitudes)).toEqual([])
  })
})

describe('Corazón del popup del mapa (DEC-94)', () => {
  it('turista: refleja el estado y marca con PUT', async () => {
    const { solicitudes } = conSesion(USUARIO_TURISTA, {
      [`GET ${FAVORITOS}`]: () => respuestaJson(200, listaElementos([FAVORITO_CASCADA])),
      [`PUT ${FAVORITOS}/Servicio/s-hotel`]: () => respuestaJson(201, FAVORITO_HOTEL),
    })
    renderApp('/explorar')
    await waitFor(async () => expect(await corazon('Cascada El Salto')).toHaveAttribute('aria-pressed', 'true'), LENTO)

    expect(within(await popupDe('Cascada El Salto')).getByRole('button', { name: 'Guardar en favoritos: Cascada El Salto' })).toHaveAttribute('aria-pressed', 'true')
    const boton = within(await popupDe('Hotel Casa Tayrona')).getByRole('button', { name: 'Guardar en favoritos: Hotel Casa Tayrona' })
    expect(boton).toHaveAttribute('aria-pressed', 'false')

    fireEvent.click(boton)

    expect(boton).toHaveAttribute('aria-pressed', 'true')
    await waitFor(() => expect(solicitudes.some((s) => s.metodo === 'PUT' && s.url === `${FAVORITOS}/Servicio/s-hotel`)).toBe(true))
    // La tarjeta se pone al día con el mismo estado compartido.
    await waitFor(async () => expect(await corazon('Hotel Casa Tayrona')).toHaveAttribute('aria-pressed', 'true'), LENTO)
  })

  it('visitante: el corazón del popup lleva al login con retorno', async () => {
    simularFetch({ ...OFERTA })
    renderApp('/explorar?texto=hotel')
    await corazon('Hotel Casa Tayrona')

    const boton = within(await popupDe('Hotel Casa Tayrona')).getByRole('button', { name: 'Guardar en favoritos: Hotel Casa Tayrona' })
    expect(boton).not.toHaveAttribute('aria-pressed')
    fireEvent.click(boton)

    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
  })

  it('otro rol: el popup no tiene corazón', async () => {
    conSesion(USUARIO_PRESTADOR)
    renderApp('/explorar')
    await screen.findByRole('button', { name: 'Menú de usuario de Casa Tayrona SAS' })
    await screen.findByRole('heading', { name: 'Hotel Casa Tayrona' }, LENTO)

    const popup = await popupDe('Hotel Casa Tayrona')
    expect(within(popup).queryByRole('button', { name: /Guardar en favoritos/ })).not.toBeInTheDocument()
    // La ventana emergente no está montada en el documento: se revisa su contenido.
    expect(within(popup).getByRole('link', { name: 'Ver detalle' })).toHaveAttribute('href', '/servicios/s-hotel')
  })
})
