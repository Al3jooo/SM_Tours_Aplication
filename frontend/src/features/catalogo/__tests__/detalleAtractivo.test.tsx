import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  ID_ATRACTIVO,
  USUARIO_TURISTA,
  atractivoPublico,
  respuestaJson,
  respuestaProblema,
  simularFetch,
} from '../../../test/fetchSimulado'
import { renderApp } from '../../../test/renderApp'

/** WF-06 para atractivos (CU «Consultar detalle de atractivo»; DEC-217, DEC-224). */
const FICHA = `GET /api/v1/catalogo/atractivos/${ID_ATRACTIVO}`
const LENTO = { timeout: 15_000 }

describe('Ficha pública de un atractivo', () => {
  it('muestra el estado, la accesibilidad, la capacidad, las normas y el horario con sus saltos de línea', async () => {
    simularFetch({ [FICHA]: () => respuestaJson(200, atractivoPublico({ condicionesAccesibilidad: ['MovilidadReducida', 'Visual'] })) })
    renderApp(`/atractivos/${ID_ATRACTIVO}`)

    expect(await screen.findByRole('heading', { level: 1, name: 'Cascada El Salto' }, LENTO)).toBeInTheDocument()
    expect(screen.getByText('Atractivo · Naturaleza')).toBeInTheDocument()
    expect(screen.getByText('Abierto')).toHaveClass('insignia-estado--Abierto')
    const accesibilidad = within(screen.getByRole('list', { name: 'Accesibilidad' }))
    expect(accesibilidad.getByRole('img', { name: 'Movilidad reducida' })).toBeInTheDocument()
    expect(accesibilidad.getByRole('img', { name: 'Discapacidad visual' })).toBeInTheDocument()
    expect(screen.getByText('Capacidad de carga')).toBeInTheDocument()
    expect(screen.getByText('80 personas')).toBeInTheDocument()
    expect(within(screen.getByRole('region', { name: 'Normas ambientales' })).getByText('Prohibido hacer fogatas.')).toBeInTheDocument()
    const horario = within(screen.getByRole('region', { name: 'Horarios de atención' })).getByText(/Martes a domingo/)
    expect(horario.textContent).toBe('Martes a domingo\n8:00 a 16:00')
    expect(horario).toHaveClass('detalle__horario')
    expect(screen.getByRole('region', { name: 'Mapa con la ubicación de Cascada El Salto' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /reservar/i })).not.toBeInTheDocument()
  })

  it('sin normas ni horario, sus regiones quedan deshabilitadas', async () => {
    simularFetch({ [FICHA]: () => respuestaJson(200, atractivoPublico({ normasAmbientales: null, horarioAtencion: null })) })
    renderApp(`/atractivos/${ID_ATRACTIVO}`)

    await screen.findByRole('heading', { level: 1, name: 'Cascada El Salto' }, LENTO)
    expect(screen.getByRole('group', { name: 'Normas ambientales' })).toHaveAttribute('aria-disabled', 'true')
    expect(screen.getByRole('group', { name: 'Horarios de atención' })).toHaveAttribute('aria-disabled', 'true')
  })

  it('C5: con sesión iniciada, la ficha sale sin Authorization y las imágenes no pasan por fetch', async () => {
    sessionStorage.setItem('samaria360.token', 'token-guardado')
    const { solicitudes } = simularFetch({
      'GET /api/v1/auth/me': () => respuestaJson(200, USUARIO_TURISTA),
      [FICHA]: () =>
        respuestaJson(
          200,
          atractivoPublico({
            imagenes: ['/api/v1/catalogo/imagenes/6b2000000000000000000001', '/api/v1/catalogo/imagenes/6b2000000000000000000002'],
          }),
        ),
    })
    renderApp(`/atractivos/${ID_ATRACTIVO}`)

    await screen.findByRole('heading', { level: 1, name: 'Cascada El Salto' }, LENTO)
    const ficha = solicitudes.filter((s) => s.url === `/api/v1/catalogo/atractivos/${ID_ATRACTIVO}`)
    expect(ficha).toHaveLength(1)
    expect(ficha[0].cabeceras.Authorization).toBeUndefined()
    // Las fotos son <img src>: el navegador las pide sin el Bearer (nunca pasan por fetch ni por httpClient).
    expect(solicitudes.some((s) => s.url.startsWith('/api/v1/catalogo/imagenes/'))).toBe(false)
    expect(screen.getByRole('img', { name: 'Foto 2 de Cascada El Salto' })).toHaveAttribute(
      'src',
      '/api/v1/catalogo/imagenes/6b2000000000000000000002',
    )
  })

  it('un 404 ATRACTIVO_NO_ENCONTRADO dice que el atractivo no está disponible, sin «Reintentar»', async () => {
    simularFetch({ [FICHA]: () => respuestaProblema(404, 'ATRACTIVO_NO_ENCONTRADO') })
    renderApp(`/atractivos/${ID_ATRACTIVO}`)

    expect(await screen.findByText('Este atractivo no está disponible.', {}, LENTO)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument()
  })

  it('un error del servidor ofrece «Reintentar»', async () => {
    let intentos = 0
    simularFetch({
      [FICHA]: () => (++intentos === 1 ? respuestaProblema(500, 'ERROR_INTERNO') : respuestaJson(200, atractivoPublico())),
    })
    const { usuario } = renderApp(`/atractivos/${ID_ATRACTIVO}`)

    await usuario.click(await screen.findByRole('button', { name: 'Reintentar' }, LENTO))
    expect(await screen.findByRole('heading', { level: 1, name: 'Cascada El Salto' })).toBeInTheDocument()
  })
})
