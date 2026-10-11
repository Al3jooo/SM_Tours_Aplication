import { screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  USUARIO_TURISTA,
  paginaOferta,
  respuestaJson,
  respuestaProblema,
  simularFetch,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

const ME = 'GET /api/v1/auth/me'
// El inicio (WF-04) consulta los destacados del catálogo.
const CATALOGO = { 'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([])) }

function sesionGuardada(token = 'token-guardado') {
  sessionStorage.setItem('samaria360.token', token)
}

describe('Sesión', () => {
  it('una sesión guardada se restaura con /me', async () => {
    sesionGuardada()
    const { solicitudes } = simularFetch({ [ME]: () => respuestaJson(200, USUARIO_TURISTA), ...CATALOGO })
    renderApp('/')

    // Antes esperaba el título «Hola, Ana Pérez» del inicio provisional, que ya no existe.
    expect(await screen.findByRole('button', { name: 'Menú de usuario de Ana Pérez' })).toHaveTextContent('AP')
    const me = solicitudes.find((s) => s.url === '/api/v1/auth/me')
    expect(me?.cabeceras.Authorization).toBe('Bearer token-guardado')
  })

  // «/» ahora es pública: la sesión se cierra y el usuario se queda en la página, con un aviso (antes iba al login).
  it('un 401 de /me en una página pública cierra la sesión y avisa sin sacar al usuario', async () => {
    sesionGuardada()
    simularFetch({ [ME]: () => respuestaProblema(401, 'NO_AUTENTICADO'), ...CATALOGO })
    const { usuario } = renderApp('/')

    const aviso = (await screen.findByText(/Tu sesión expiró\./)).closest('[role="status"]')
    expect(aviso).not.toBeNull()
    expect(screen.getByRole('link', { name: 'Inicia sesión de nuevo' })).toHaveAttribute('href', '/login')
    expect(ubicacionActual()).toBe('/')
    expect(sessionStorage.getItem('samaria360.token')).toBeNull()
    expect(screen.queryByRole('button', { name: /Menú de usuario/ })).not.toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Cerrar aviso' }))
    expect(screen.queryByText('Tu sesión expiró.', { exact: false })).not.toBeInTheDocument()
  })

  it('un 403 de /me (cuenta desactivada) cierra la sesión y avisa que la cuenta no está activa', async () => {
    sesionGuardada()
    simularFetch({ [ME]: () => respuestaProblema(403, 'CUENTA_INACTIVA'), ...CATALOGO })
    renderApp('/')

    expect(await screen.findByText('Tu cuenta no está activa.', { exact: false })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Iniciar sesión' })).toHaveAttribute('href', '/login')
    expect(ubicacionActual()).toBe('/')
    expect(sessionStorage.getItem('samaria360.token')).toBeNull()
  })

  it('Cerrar sesión desde el menú del avatar borra el token y vuelve al login (RF55)', async () => {
    sesionGuardada()
    simularFetch({ [ME]: () => respuestaJson(200, USUARIO_TURISTA) })
    const { usuario } = renderApp('/')

    await usuario.click(await screen.findByRole('button', { name: 'Menú de usuario de Ana Pérez' }))
    await usuario.click(screen.getByRole('menuitem', { name: 'Cerrar sesión' }))

    expect(await screen.findByLabelText('Correo electrónico')).toBeInTheDocument()
    expect(sessionStorage.getItem('samaria360.token')).toBeNull()
    expect(screen.queryByRole('button', { name: /Menú de usuario/ })).not.toBeInTheDocument()
  })

  it('el menú del avatar se abre con Enter y se cierra con Escape devolviendo el foco', async () => {
    sesionGuardada()
    simularFetch({ [ME]: () => respuestaJson(200, USUARIO_TURISTA) })
    const { usuario } = renderApp('/')

    const avatar = await screen.findByRole('button', { name: 'Menú de usuario de Ana Pérez' })
    avatar.focus()
    await usuario.keyboard('{Enter}')

    const opcion = screen.getByRole('menuitem', { name: 'Cerrar sesión' })
    await waitFor(() => expect(opcion).toHaveFocus())
    expect(avatar).toHaveAttribute('aria-expanded', 'true')

    await usuario.keyboard('{Escape}')

    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
    expect(avatar).toHaveFocus()
    expect(avatar).toHaveAttribute('aria-expanded', 'false')
  })
})
