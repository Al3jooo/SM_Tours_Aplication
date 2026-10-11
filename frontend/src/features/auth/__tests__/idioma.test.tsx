import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { respuestaProblema, simularFetch } from '../../../test/fetchSimulado'
import { renderApp } from '../../../test/renderApp'

describe('Cambio de idioma (RF03, RNF08)', () => {
  // Límite de 15 s: medida en más de 2 s (primer render del archivo, 23 pulsaciones de usuario.type y cambio de idioma).
  it('traduce los textos y conserva el correo escrito', async () => {
    simularFetch({})
    const { usuario } = renderApp('/login')

    await usuario.type(screen.getByLabelText('Correo electrónico'), 'turista@samaria360.test')
    await usuario.click(screen.getByRole('button', { name: 'Inglés' }))

    expect(screen.getByLabelText('Email')).toHaveValue('turista@samaria360.test')
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'English' })).toHaveAttribute('aria-pressed', 'true')
    expect(document.documentElement.lang).toBe('en')
  }, 15_000)

  // Límite de 15 s: medida en más de 2 s (33 pulsaciones de usuario.type y cambio de idioma).
  it('el mensaje de error mostrado también se traduce', async () => {
    simularFetch({ 'POST /api/v1/auth/login': () => respuestaProblema(401, 'CREDENCIALES_INVALIDAS') })
    const { usuario } = renderApp('/login')

    await usuario.type(screen.getByLabelText('Correo electrónico'), 'turista@samaria360.test')
    await usuario.type(screen.getByLabelText('Contraseña'), 'incorrecta')
    await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    expect(await screen.findByText('Correo o contraseña incorrectos.')).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Inglés' }))

    expect(screen.getByText('Incorrect email or password.')).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toHaveValue('incorrecta')
  }, 15_000)
})
