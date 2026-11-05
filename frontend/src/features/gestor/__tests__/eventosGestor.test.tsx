import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  USUARIO_GESTOR,
  eventoGestion,
  paginaEventos,
  respuestaJson,
  respuestaProblema,
  simularFetch,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

/** WF-25 «Eventos» (flujo «Consultar eventos registrados»; DEC-238, DEC-247). */
const ME = 'GET /api/v1/auth/me'
const LISTA = 'GET /api/v1/catalogo/gestor/eventos'
const LENTO = { timeout: 15_000 }
const plano = (texto: string | null) => (texto ?? '').replace(/\s+/g, ' ')

function conGestor(rutas: Parameters<typeof simularFetch>[0]) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  return simularFetch({ [ME]: () => respuestaJson(200, USUARIO_GESTOR), ...rutas })
}

describe('WF-25 «Eventos» (panel del gestor)', () => {
  it('lista todos los eventos con fechas de día, ubicación y registro, sin acciones', async () => {
    const vencido = eventoGestion({
      id: '6b3000000000000000000002',
      nombre: 'Feria del Libro',
      fechaInicio: '2026-01-10',
      fechaFin: '2026-01-10',
      ubicacion: 'Biblioteca Banco de la República',
      fechaRegistro: '2026-01-02T04:00:00Z',
    })
    const { solicitudes } = conGestor({ [LISTA]: () => respuestaJson(200, paginaEventos([eventoGestion(), vencido])) })
    renderApp('/gestor/eventos')

    const tabla = await screen.findByRole('table', { name: 'Eventos registrados' }, LENTO)
    expect(screen.getByText('2 eventos')).toBeInTheDocument()
    const filas = within(tabla).getAllByRole('row').slice(1)
    expect(filas.map((fila) => within(fila).getByRole('rowheader').textContent)).toEqual(['Fiestas del Mar', 'Feria del Libro'])
    const celdas = within(filas[1]).getAllByRole('cell').map((celda) => plano(celda.textContent))
    // Un evento de un día; el registro es un instante en la hora de Santa Marta (04:00 UTC del 2 es el 1 en Bogotá).
    expect(celdas).toEqual(['10 de ene de 2026', 'Biblioteca Banco de la República', '01/01/2026'])
    expect(plano(within(filas[0]).getAllByRole('cell')[0].textContent)).toBe('20 a 22 de nov de 2026')
    expect(within(tabla).queryByRole('link')).not.toBeInTheDocument()
    expect(within(tabla).queryByRole('button')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Registrar evento' })).toHaveAttribute('href', '/gestor/eventos/nuevo')
    const consulta = solicitudes.find((s) => s.url.startsWith('/api/v1/catalogo/gestor/eventos'))!
    expect(consulta.url).toBe('/api/v1/catalogo/gestor/eventos?pagina=0')
    expect(consulta.cabeceras.Authorization).toBe('Bearer token-guardado')
  })

  it('la página va en la URL (desde 1) y en la API desde 0', async () => {
    const elementos = Array.from({ length: 10 }, (_, i) =>
      eventoGestion({ id: `6b30000000000000000000${String(i).padStart(2, '0')}`, nombre: `Evento ${i}` }),
    )
    const { solicitudes } = conGestor({
      [LISTA]: (solicitud) =>
        respuestaJson(200, paginaEventos(solicitud.url.endsWith('pagina=1') ? [eventoGestion()] : elementos, 0, 11)),
    })
    const { usuario } = renderApp('/gestor/eventos')

    await screen.findByText('11 eventos', {}, LENTO)
    await usuario.click(screen.getByRole('button', { name: /Página 2/ }))
    await waitFor(() => expect(ubicacionActual()).toBe('/gestor/eventos?pagina=2'))
    expect(solicitudes.some((s) => s.url === '/api/v1/catalogo/gestor/eventos?pagina=1')).toBe(true)
  })

  it('sin eventos, lo dice; un error ofrece «Reintentar»', async () => {
    let intentos = 0
    conGestor({
      [LISTA]: () => (++intentos === 1 ? respuestaProblema(500, 'ERROR_INTERNO') : respuestaJson(200, paginaEventos([]))),
    })
    const { usuario } = renderApp('/gestor/eventos')

    expect(await screen.findByText('No se pudieron cargar los eventos.', {}, LENTO)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByText('Todavía no hay eventos registrados.')).toBeInTheDocument()
  })

  it('una página fuera de rango vuelve a la última', async () => {
    conGestor({ [LISTA]: () => respuestaJson(200, paginaEventos([], 4, 3)) })
    renderApp('/gestor/eventos?pagina=5')

    await waitFor(() => expect(ubicacionActual()).toBe('/gestor/eventos'), LENTO)
  })

  it('un 403 muestra «Sin permiso»', async () => {
    conGestor({ [LISTA]: () => respuestaProblema(403, 'ACCESO_DENEGADO') })
    renderApp('/gestor/eventos')

    expect(await screen.findByRole('heading', { name: 'Sin permiso' }, LENTO)).toBeInTheDocument()
  })
})
