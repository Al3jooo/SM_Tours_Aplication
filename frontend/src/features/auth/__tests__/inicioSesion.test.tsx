import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  SESION_TURISTA,
  USUARIO_TURISTA,
  respuestaJson,
  respuestaProblema,
  simularFetch,
} from '../../../test/fetchSimulado'
import { renderApp } from '../../../test/renderApp'

const LOGIN = 'POST /api/v1/auth/login'
const ME = 'GET /api/v1/auth/me'

async function llenarYEnviar(usuario: ReturnType<typeof renderApp>['usuario'], correo: string, contrasena: string) {
  await usuario.type(screen.getByLabelText('Correo electrónico'), correo)
  await usuario.type(screen.getByLabelText('Contraseña'), contrasena)
  await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
}

describe('Inicio de sesión (RF15)', () => {
  it('un login exitoso redirige al inicio y muestra el avatar con las iniciales', async () => {
    const { solicitudes } = simularFetch({
      [LOGIN]: () => respuestaJson(200, SESION_TURISTA),
      [ME]: () => respuestaJson(200, USUARIO_TURISTA),
    })
    const { usuario } = renderApp('/login')

    await llenarYEnviar(usuario, 'turista@samaria360.test', 'Samaria360*')

    expect(await screen.findByRole('heading', { level: 1, name: 'Hola, Ana Pérez' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Menú de usuario de Ana Pérez' })).toHaveTextContent('AP')
    expect(sessionStorage.getItem('samaria360.token')).toBe(SESION_TURISTA.token)

    const login = solicitudes.find((s) => s.url === '/api/v1/auth/login')
    expect(login?.cuerpo).toEqual({ correo: 'turista@samaria360.test', contrasena: 'Samaria360*' })
    const me = solicitudes.find((s) => s.url === '/api/v1/auth/me')
    expect(me?.cabeceras.Authorization).toBe(`Bearer ${SESION_TURISTA.token}`)
  })

  it('un 401 muestra el mensaje de credenciales inválidas', async () => {
    simularFetch({ [LOGIN]: () => respuestaProblema(401, 'CREDENCIALES_INVALIDAS') })
    const { usuario } = renderApp('/login')

    await llenarYEnviar(usuario, 'turista@samaria360.test', 'incorrecta')

    const alerta = screen.getByRole('alert')
    expect(await within(alerta).findByText('Correo o contraseña incorrectos.')).toBeInTheDocument()
    expect(alerta).toHaveAttribute('aria-live', 'assertive')
    expect(sessionStorage.getItem('samaria360.token')).toBeNull()
  })

  it('un 423 muestra la hora de desbloqueo en la hora local del navegador', async () => {
    const bloqueadoHasta = '2026-10-11T15:45:00Z'
    simularFetch({ [LOGIN]: () => respuestaProblema(423, 'CUENTA_BLOQUEADA', { bloqueadoHasta }) })
    const { usuario } = renderApp('/login')

    await llenarYEnviar(usuario, 'turista@samaria360.test', 'incorrecta')

    // Misma conversión que la app (Intl con la zona horaria del entorno): la prueba no depende de dónde corre.
    const hora = new Intl.DateTimeFormat('es', { timeStyle: 'short' }).format(new Date(bloqueadoHasta))
    expect(await screen.findByText(/Tu cuenta está bloqueada/)).toHaveTextContent(hora)
  })

  it('un 403 muestra el mensaje de cuenta inactiva', async () => {
    simularFetch({ [LOGIN]: () => respuestaProblema(403, 'CUENTA_INACTIVA') })
    const { usuario } = renderApp('/login')

    await llenarYEnviar(usuario, 'turista@samaria360.test', 'Samaria360*')

    expect(await screen.findByText(/Tu cuenta no está activa/)).toBeInTheDocument()
  })

  it('un 400 muestra el mensaje de datos inválidos', async () => {
    simularFetch({ [LOGIN]: () => respuestaProblema(400, 'DATOS_INVALIDOS') })
    const { usuario } = renderApp('/login')

    await llenarYEnviar(usuario, 'no-es-correo', 'Samaria360*')

    expect(await screen.findByText(/Revisa los datos/)).toBeInTheDocument()
  })

  it('un error de red muestra su propio mensaje', async () => {
    simularFetch({
      [LOGIN]: () => {
        throw new TypeError('Failed to fetch')
      },
    })
    const { usuario } = renderApp('/login')

    await llenarYEnviar(usuario, 'turista@samaria360.test', 'Samaria360*')

    expect(await screen.findByText(/No fue posible conectar con el servidor/)).toBeInTheDocument()
  })

  it('con campos vacíos no llama al backend y marca los campos', async () => {
    const { fetchSimulado } = simularFetch({})
    const { usuario } = renderApp('/login')

    await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }))

    const correo = screen.getByLabelText('Correo electrónico')
    expect(correo).toHaveAttribute('aria-invalid', 'true')
    expect(correo).toHaveAccessibleDescription('Este campo es obligatorio.')
    expect(correo).toHaveFocus()
    expect(fetchSimulado).not.toHaveBeenCalled()
  })

  it('Registrarse y ¿Olvidaste tu contraseña? están visibles pero deshabilitados', () => {
    simularFetch({})
    renderApp('/login')

    expect(screen.getByRole('button', { name: /Registrarse/ })).toHaveAttribute('aria-disabled', 'true')
    expect(screen.getByRole('button', { name: /¿Olvidaste tu contraseña\?/ })).toHaveAttribute('aria-disabled', 'true')
    expect(screen.getByText('3 intentos fallidos bloquearán la cuenta por 15 minutos.')).toBeInTheDocument()
  })
})
