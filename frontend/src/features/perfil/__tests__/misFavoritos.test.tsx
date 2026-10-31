import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  FAVORITO_CASCADA,
  FAVORITO_HOTEL,
  FAVORITO_TOUR_NO_DISPONIBLE,
  HOTEL,
  USUARIO_TURISTA,
  listaElementos,
  paginaOferta,
  respuestaJson,
  respuestaProblema,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

const FAVORITOS = '/api/v1/catalogo/favoritos'

/** La primera carga de una página diferida puede pasar de 5 s con toda la suite en paralelo (ver test/setup.ts). */
const LENTO = { timeout: 15_000 }

function comoTurista(rutas: Parameters<typeof simularFetch>[0]) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  return simularFetch({ 'GET /api/v1/auth/me': () => respuestaJson(200, USUARIO_TURISTA), ...rutas })
}

const listas = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.metodo === 'GET' && s.url === FAVORITOS)
const tarjeta = (nombre: string) => screen.getByRole('article', { name: nombre })
const nombres = () => screen.getAllByRole('article').map((articulo) => within(articulo).getByRole('heading').textContent)
const LISTA = () => respuestaJson(200, listaElementos([FAVORITO_HOTEL, FAVORITO_CASCADA, FAVORITO_TOUR_NO_DISPONIBLE]))

describe('WF-17 «Mis favoritos» (RF54)', () => {
  it('lista con el contador, la insignia «No disponible» y «Ver detalle» en los favoritos disponibles', async () => {
    const { solicitudes } = comoTurista({ [`GET ${FAVORITOS}`]: LISTA })
    renderApp('/perfil/favoritos')

    expect(await screen.findByText(/3 de 100 favoritos/, {}, LENTO)).toBeInTheDocument()
    expect(nombres()).toEqual([
      'Hotel Casa Tayrona',
      'Cascada El Salto',
      'Tour Sierra Nevada',
    ])
    expect(listas(solicitudes)[0].cabeceras.Authorization).toBe('Bearer token-guardado')

    const hotel = tarjeta('Hotel Casa Tayrona')
    expect(within(hotel).getByText('Disponible')).toBeInTheDocument()
    expect(within(hotel).getByRole('link', { name: 'Ver detalle' })).toHaveAttribute('href', '/servicios/s-hotel')

    // Atractivo: «Ver detalle» lleva a su ficha (iteración E, B1; DEC-87, DEC-224).
    const cascada = tarjeta('Cascada El Salto')
    expect(within(cascada).getByText('Atractivo · Naturaleza')).toBeInTheDocument()
    expect(within(cascada).getByRole('link', { name: 'Ver detalle' })).toHaveAttribute('href', '/atractivos/a-cascada')

    const tour = tarjeta('Tour Sierra Nevada')
    expect(within(tour).getByText('No disponible')).toBeInTheDocument()
    expect(within(tour).queryByRole('link')).not.toBeInTheDocument()
    expect(within(tour).queryByRole('button', { name: /Ver detalle/ })).not.toBeInTheDocument()
    expect(within(tour).getByRole('button', { name: 'Quitar Tour Sierra Nevada de favoritos' })).toBeInTheDocument()
  })

  it('un favorito que ya no existe se muestra sin nombre del catálogo', async () => {
    comoTurista({
      [`GET ${FAVORITOS}`]: () =>
        respuestaJson(200, listaElementos([{ ...FAVORITO_TOUR_NO_DISPONIBLE, nombre: null, categoria: null, imagen: null }])),
    })
    renderApp('/perfil/favoritos')

    expect(await screen.findByRole('heading', { level: 2, name: 'Ya no está en el catálogo' }, LENTO)).toBeInTheDocument()
  })

  it('«Quitar» envía DELETE con el token, actualiza la lista y el contador, anuncia y mueve el foco', async () => {
    const { solicitudes } = comoTurista({
      [`GET ${FAVORITOS}`]: LISTA,
      [`DELETE ${FAVORITOS}/Servicio/s-hotel`]: () => new Response(null, { status: 204 }),
    })
    const { usuario } = renderApp('/perfil/favoritos')

    await usuario.click(await screen.findByRole('button', { name: 'Quitar Hotel Casa Tayrona de favoritos' }, LENTO))

    expect((await screen.findByText('Quitado de favoritos.')).closest('[role="status"]')).not.toBeNull()
    expect(screen.queryByRole('article', { name: 'Hotel Casa Tayrona' })).not.toBeInTheDocument()
    expect(screen.getByText(/2 de 100 favoritos/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Quitar Cascada El Salto de favoritos' })).toHaveFocus()
    const borrado = solicitudes.find((s) => s.metodo === 'DELETE')
    expect(borrado?.url).toBe(`${FAVORITOS}/Servicio/s-hotel`)
    expect(borrado?.cabeceras.Authorization).toBe('Bearer token-guardado')
  })

  it('lo quitado en WF-17 queda desmarcado en el corazón de WF-05, sin volver a pedir la lista (DEC-93)', async () => {
    const { solicitudes } = comoTurista({
      [`GET ${FAVORITOS}`]: LISTA,
      [`DELETE ${FAVORITOS}/Servicio/s-hotel`]: () => new Response(null, { status: 204 }),
      'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([HOTEL])),
    })
    const { usuario } = renderApp('/perfil/favoritos')

    await usuario.click(await screen.findByRole('button', { name: 'Quitar Hotel Casa Tayrona de favoritos' }, LENTO))
    await screen.findByText('Quitado de favoritos.')
    await usuario.click(screen.getByRole('link', { name: 'Búsqueda' }))

    const corazon = await screen.findByRole('button', { name: 'Guardar en favoritos: Hotel Casa Tayrona' }, LENTO)
    expect(corazon).toHaveAttribute('aria-pressed', 'false')
    expect(listas(solicitudes)).toHaveLength(1)
    // El anuncio de WF-17 no pasa a la pantalla siguiente.
    expect(screen.queryByText('Quitado de favoritos.')).not.toBeInTheDocument()
  })

  it('si DELETE falla, el favorito vuelve a su lugar y se avisa el error', async () => {
    comoTurista({
      [`GET ${FAVORITOS}`]: LISTA,
      [`DELETE ${FAVORITOS}/Atractivo/a-cascada`]: () => respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE'),
    })
    const { usuario } = renderApp('/perfil/favoritos')

    await usuario.click(await screen.findByRole('button', { name: 'Quitar Cascada El Salto de favoritos' }, LENTO))

    expect(await screen.findByText('No fue posible actualizar tus favoritos. Inténtalo de nuevo.')).toBeInTheDocument()
    await waitFor(() => expect(nombres()).toHaveLength(3))
    expect(nombres()).toEqual([
      'Hotel Casa Tayrona',
      'Cascada El Salto',
      'Tour Sierra Nevada',
    ])
  })

  it('sin favoritos muestra el estado vacío con «Explorar la oferta»', async () => {
    comoTurista({ [`GET ${FAVORITOS}`]: () => respuestaJson(200, listaElementos([])) })
    renderApp('/perfil/favoritos')

    expect(await screen.findByText('Aún no tienes favoritos.', {}, LENTO)).toBeInTheDocument()
    expect(screen.getByText('Marca el corazón en los resultados de la búsqueda o en la ficha de un servicio.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Explorar la oferta' })).toHaveAttribute('href', '/explorar')
    expect(screen.queryByText(/de 100 favoritos/)).not.toBeInTheDocument()
  })

  it('un error de carga no queda fijo: «Reintentar» vuelve a pedir la lista (DEC-93)', async () => {
    let intentos = 0
    const { solicitudes } = comoTurista({
      [`GET ${FAVORITOS}`]: () => {
        intentos += 1
        return intentos === 1 ? respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE') : LISTA()
      },
    })
    const { usuario } = renderApp('/perfil/favoritos')

    expect(await screen.findByText('No fue posible cargar tus favoritos.', {}, LENTO)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByText(/3 de 100 favoritos/)).toBeInTheDocument()
    expect(listas(solicitudes)).toHaveLength(2)
  })

  it('«Volver a mi perfil» lleva a WF-16', async () => {
    comoTurista({
      [`GET ${FAVORITOS}`]: () => respuestaJson(200, listaElementos([])),
      'GET /api/v1/auth/perfil': () => respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE'),
      'GET /api/v1/auth/paises': () => respuestaJson(200, ['CO']),
    })
    const { usuario } = renderApp('/perfil/favoritos')

    await usuario.click(await screen.findByRole('link', { name: /Volver a mi perfil/ }, LENTO))
    await waitFor(() => expect(ubicacionActual()).toBe('/perfil'))
  })
})
