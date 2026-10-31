import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  ID_ATRACTIVO,
  USUARIO_GESTOR,
  elementoAtractivo,
  paginaAtractivos,
  respuestaJson,
  respuestaProblema,
  simularFetch,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

/** WF-24 «Atractivos» (flujo «Consultar atractivos»; DEC-206, DEC-222). */
const ME = 'GET /api/v1/auth/me'
const LISTA = 'GET /api/v1/catalogo/gestor/atractivos'
const LENTO = { timeout: 15_000 }

function conGestor(rutas: Parameters<typeof simularFetch>[0]) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  return simularFetch({ [ME]: () => respuestaJson(200, USUARIO_GESTOR), ...rutas })
}

describe('WF-24 «Atractivos» (panel del gestor)', () => {
  it('lista todos los atractivos en una tabla con portada, estado, capacidad y acciones', async () => {
    const otro = elementoAtractivo({
      id: '6b1000000000000000000002',
      nombre: 'Mirador de Taganga',
      estadoOperativo: 'En_mantenimiento',
      limiteCapacidadCarga: 1,
      imagen: null,
    })
    const { solicitudes } = conGestor({ [LISTA]: () => respuestaJson(200, paginaAtractivos([elementoAtractivo(), otro])) })
    renderApp('/gestor/atractivos')

    const tabla = await screen.findByRole('table', { name: 'Atractivos registrados' }, LENTO)
    expect(screen.getByText('2 atractivos')).toBeInTheDocument()
    const filas = within(tabla).getAllByRole('row').slice(1)
    expect(filas.map((fila) => within(fila).getByRole('rowheader').textContent)).toEqual(['Cascada El Salto', 'Mirador de Taganga'])
    const mirador = within(filas[1])
    expect(mirador.getByText('En mantenimiento')).toHaveClass('insignia-estado--En_mantenimiento')
    expect(mirador.getByText('1 persona')).toBeInTheDocument()
    expect(mirador.getByRole('link', { name: 'Editar Mirador de Taganga' })).toHaveAttribute(
      'href',
      '/gestor/atractivos/6b1000000000000000000002',
    )
    expect(mirador.getByRole('link', { name: 'Ver la ficha pública de Mirador de Taganga' })).toHaveAttribute(
      'href',
      '/atractivos/6b1000000000000000000002',
    )
    expect(mirador.getByRole('button', { name: 'Cambiar el estado de Mirador de Taganga' })).toBeInTheDocument()
    const consultas = solicitudes.filter((s) => s.url.startsWith('/api/v1/catalogo/gestor/atractivos'))
    expect(consultas[0].url).toBe('/api/v1/catalogo/gestor/atractivos?pagina=0')
    expect(consultas[0].cabeceras.Authorization).toBe('Bearer token-guardado')
  })

  it('los filtros se ven deshabilitados y listados como pendientes (G2, DEC-222)', async () => {
    conGestor({ [LISTA]: () => respuestaJson(200, paginaAtractivos([elementoAtractivo()])) })
    renderApp('/gestor/atractivos')

    await screen.findByRole('table', {}, LENTO)
    const filtros = screen.getByRole('group', { name: 'Filtros' })
    expect(filtros).toHaveAttribute('aria-disabled', 'true')
    expect(within(filtros).getByLabelText('Estado')).toBeDisabled()
    expect(within(filtros).getByLabelText('Buscar por nombre')).toBeDisabled()
    expect(within(filtros).getByText('Los filtros estarán disponibles más adelante.')).toBeInTheDocument()
  })

  it('la página va en la URL (desde 1) y en la API desde 0', async () => {
    const elementos = Array.from({ length: 10 }, (_, indice) =>
      elementoAtractivo({ id: `6b10000000000000000000${String(indice).padStart(2, '0')}`, nombre: `Atractivo ${indice}` }),
    )
    const { solicitudes } = conGestor({
      [LISTA]: (solicitud) =>
        respuestaJson(200, paginaAtractivos(solicitud.url.endsWith('pagina=1') ? [elementoAtractivo()] : elementos, 0, 11)),
    })
    const { usuario } = renderApp('/gestor/atractivos')

    await screen.findByText('11 atractivos', {}, LENTO)
    await usuario.click(screen.getByRole('button', { name: /Página 2/ }))
    await waitFor(() => expect(ubicacionActual()).toBe('/gestor/atractivos?pagina=2'))
    expect(solicitudes.some((s) => s.url === '/api/v1/catalogo/gestor/atractivos?pagina=1')).toBe(true)
  })

  it('sin atractivos, lo dice; un error ofrece «Reintentar»', async () => {
    let intentos = 0
    conGestor({
      [LISTA]: () => (++intentos === 1 ? respuestaProblema(500, 'ERROR_INTERNO') : respuestaJson(200, paginaAtractivos([]))),
    })
    const { usuario } = renderApp('/gestor/atractivos')

    await usuario.click(await screen.findByRole('button', { name: 'Reintentar' }, LENTO))
    expect(await screen.findByText('Todavía no hay atractivos registrados.')).toBeInTheDocument()
  })

  it('al volver del registro muestra el anuncio una vez', async () => {
    conGestor({ [LISTA]: () => respuestaJson(200, paginaAtractivos([elementoAtractivo()])) })
    renderApp({ pathname: '/gestor/atractivos', state: { anuncio: 'registrado' } })

    expect(await screen.findByText('Atractivo registrado: ya es visible en la búsqueda.', {}, LENTO)).toBeInTheDocument()
    // El anuncio llega con la página; la tabla, con la respuesta del listado.
    expect(await screen.findByRole('link', { name: 'Editar Cascada El Salto' }, LENTO)).toHaveAttribute(
      'href',
      `/gestor/atractivos/${ID_ATRACTIVO}`,
    )
  })
})
