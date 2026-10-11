import { screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { USUARIO_TURISTA, respuestaJson, respuestaProblema, simularFetch } from '../../../test/fetchSimulado'
import { renderApp } from '../../../test/renderApp'

const ME = 'GET /api/v1/auth/me'

function sesionGuardada(token = 'token-guardado') {
  sessionStorage.setItem('samaria360.token', token)
}

describe('Sesión y rutas protegidas', () => {
  it('una sesión guardada se restaura con /me', async () => {
    sesionGuardada()
    const { solicitudes } = simularFetch({ [ME]: () => respuestaJson(200, USUARIO_TURISTA) })
    renderApp('/')

    expect(await screen.findByRole('heading', { level: 1, name: 'Hola, Ana Pérez' })).toBeInTheDocument()
    expect(solicitudes[0].cabeceras.Authorization).toBe('Bearer token-guardado')
  })

  it('un 401 de /me cierra la sesión y vuelve al login', async () => {
    sesionGuardada()
    simularFetch({ [ME]: () => respuestaProblema(401, 'NO_AUTENTICADO') })
    renderApp('/')

    expect(await screen.findByRole('button', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(sessionStorage.getItem('samaria360.token')).toBeNull()
  })

  it('un 403 de /me (cuenta desactivada) también cierra la sesión', async () => {
    sesionGuardada()
    simularFetch({ [ME]: () => respuestaProblema(403, 'CUENTA_INACTIVA') })
    renderApp('/')

    expect(await screen.findByRole('button', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(sessionStorage.getItem('samaria360.token')).toBeNull()
  })

  it('sin sesión, una ruta privada redirige al login sin llamar al backend', async () => {
    const { fetchSimulado } = simularFetch({})
    renderApp('/')

    expect(await screen.findByLabelText('Correo electrónico')).toBeInTheDocument()
    expect(fetchSimulado).not.toHaveBeenCalled()
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
