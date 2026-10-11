import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { LoginPage } from '../../features/auth/LoginPage'
import { SesionProvider } from '../../features/auth/SesionProvider'
import {
  SESION_TURISTA,
  USUARIO_TURISTA,
  respuestaJson,
  respuestaProblema,
  simularFetch,
} from '../../test/fetchSimulado'
import { RutaProtegida } from '../RutaProtegida'

/**
 * Hoy no hay rutas privadas reales (inicio y exploración son públicas): RutaProtegida se prueba con una ruta de
 * prueba, porque la necesitarán las pantallas de prestador y gestor.
 */
function renderRutaPrivada() {
  const usuario = userEvent.setup()
  render(
    <MemoryRouter initialEntries={['/privada']}>
      <SesionProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<RutaProtegida />}>
            <Route path="/privada" element={<h1>Zona privada</h1>} />
          </Route>
        </Routes>
      </SesionProvider>
    </MemoryRouter>,
  )
  return usuario
}

describe('RutaProtegida', () => {
  it('sin sesión redirige al login sin llamar al backend', async () => {
    const { fetchSimulado } = simularFetch({})
    renderRutaPrivada()

    expect(await screen.findByLabelText('Correo electrónico')).toBeInTheDocument()
    expect(fetchSimulado).not.toHaveBeenCalled()
  })

  // Límite de 15 s: medida en más de 2 s (34 pulsaciones de usuario.type y regreso a la ruta privada).
  it('después de iniciar sesión vuelve a la ruta privada pedida', async () => {
    simularFetch({
      'POST /api/v1/auth/login': () => respuestaJson(200, SESION_TURISTA),
      'GET /api/v1/auth/me': () => respuestaJson(200, USUARIO_TURISTA),
    })
    const usuario = renderRutaPrivada()

    await usuario.type(await screen.findByLabelText('Correo electrónico'), 'turista@samaria360.test')
    await usuario.type(screen.getByLabelText('Contraseña'), 'Samaria360*')
    await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }))

    expect(await screen.findByRole('heading', { name: 'Zona privada' })).toBeInTheDocument()
  }, 15_000)

  it('con una sesión válida muestra el contenido', async () => {
    sessionStorage.setItem('samaria360.token', 'token-guardado')
    simularFetch({ 'GET /api/v1/auth/me': () => respuestaJson(200, USUARIO_TURISTA) })
    renderRutaPrivada()

    expect(await screen.findByRole('heading', { name: 'Zona privada' })).toBeInTheDocument()
  })

  it('si /me responde 401, cierra la sesión y redirige al login', async () => {
    sessionStorage.setItem('samaria360.token', 'token-guardado')
    simularFetch({ 'GET /api/v1/auth/me': () => respuestaProblema(401, 'NO_AUTENTICADO') })
    renderRutaPrivada()

    expect(await screen.findByLabelText('Correo electrónico')).toBeInTheDocument()
    expect(sessionStorage.getItem('samaria360.token')).toBeNull()
  })
})
