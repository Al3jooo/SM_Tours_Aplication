import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  USUARIO_ADMIN,
  USUARIO_GESTOR,
  USUARIO_TURISTA,
  paginaOferta,
  respuestaJson,
  simularFetch,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

/** Rutas de la iteración D, sesión B (DEC-196, DEC-197): solo el Prestador las ve y las abre. */

const ME = 'GET /api/v1/auth/me'
const ID = '6a3000000000000000000001'
const RUTAS = [`/prestador/servicios/${ID}/precio`, `/prestador/servicios/${ID}/datos-comerciales`, '/prestador/perfil', '/prestador/reservas']
const LENTO = { timeout: 15_000 }

function conSesion(usuario: object) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  return simularFetch({
    [ME]: () => respuestaJson(200, usuario),
    'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([])),
  })
}

describe('Rutas nuevas del Prestador (DEC-196)', () => {
  describe.each([
    ['Turista', USUARIO_TURISTA],
    ['Gestor', USUARIO_GESTOR],
    ['Administrador', USUARIO_ADMIN],
  ])('%s', (_rol, usuarioActual) => {
    it.each(RUTAS)('en %s ve «Sin permiso» y no llama a las APIs del Prestador', async (ruta) => {
      const { solicitudes } = conSesion(usuarioActual)
      renderApp(ruta)

      expect(await screen.findByRole('heading', { name: 'Sin permiso' }, LENTO)).toBeInTheDocument()
      expect(
        solicitudes.some(
          (s) => s.url.startsWith('/api/v1/catalogo/prestador') || s.url.startsWith('/api/v1/auth/prestador') || s.url.startsWith('/api/v1/reservas/prestador'),
        ),
      ).toBe(false)
    })

    it('el menú no tiene «Historial de reservas» ni «Mi perfil comercial»', async () => {
      conSesion(usuarioActual)
      const { usuario } = renderApp('/')
      await usuario.click(await screen.findByRole('button', { name: `Menú de usuario de ${usuarioActual.nombreVisible}` }, LENTO))
      const menu = within(screen.getByRole('menu'))
      expect(menu.queryByRole('menuitem', { name: 'Historial de reservas' })).not.toBeInTheDocument()
      expect(menu.queryByRole('menuitem', { name: 'Mi perfil comercial' })).not.toBeInTheDocument()
    })
  })

  it.each(RUTAS)('sin sesión, %s lleva al login', async (ruta) => {
    simularFetch({})
    renderApp(ruta)
    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
  })
})
