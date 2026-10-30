import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  USUARIO_PRESTADOR,
  paginaOferta,
  perfilPrestador,
  respuestaJson,
  respuestaProblema,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp } from '../../../test/renderApp'
import { validarPerfilComercial } from '../PerfilComercialPage'

/** WF-22 «Mi perfil comercial» (RF43, flujo A; DEC-196, DEC-202). */

const ME = 'GET /api/v1/auth/me'
const PERFIL = '/api/v1/auth/prestador/perfil'
const LENTO = { timeout: 15_000 }

type Manejador = (solicitud: SolicitudSimulada) => Response | Promise<Response>

function montar(ruta: string, rutas: Record<string, Manejador>) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  const simulado = simularFetch({ [ME]: () => respuestaJson(200, USUARIO_PRESTADOR), ...rutas })
  return { ...simulado, ...renderApp(ruta) }
}

/** Valor de un dato (`<dt>` → `<dd>`). */
function valor(etiqueta: string) {
  return screen.getByText(etiqueta, { selector: 'dt' }).nextElementSibling as HTMLElement
}

const puts = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.metodo === 'PUT')

describe('Perfil comercial del Prestador (RF43)', () => {
  it('el menú lleva al perfil; los datos del registro van en solo lectura con la nota del correo de acceso', async () => {
    const { usuario } = montar('/', {
      'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([])),
      [`GET ${PERFIL}`]: () => respuestaJson(200, perfilPrestador({ razonSocial: null })),
    })
    await usuario.click(await screen.findByRole('button', { name: 'Menú de usuario de Casa Tayrona SAS' }))
    await usuario.click(within(screen.getByRole('menu')).getByRole('menuitem', { name: 'Mi perfil comercial' }))

    await screen.findByRole('heading', { level: 1, name: 'Mi perfil comercial' }, LENTO)
    expect(await screen.findByText('prestador@samaria360.test', {}, LENTO)).toBeInTheDocument()
    expect(valor('Correo de acceso')).toHaveTextContent('prestador@samaria360.test')
    expect(valor('Representante legal')).toHaveTextContent('Carlos Gómez')
    expect(valor('Razón social')).toHaveTextContent('—')
    expect(valor('Registro Nacional de Turismo (RNT)')).toHaveTextContent('RNT-00000')
    // El correo de acceso no es un campo editable.
    expect(screen.queryByRole('textbox', { name: /Correo de acceso/ })).not.toBeInTheDocument()
    expect(screen.getByText(/El correo de acceso es tu credencial para iniciar sesión y no se cambia aquí/)).toBeInTheDocument()
    expect(screen.getByLabelText('Correo corporativo')).toHaveValue('contacto@casatayrona.test')
    expect(screen.getByLabelText('Teléfono')).toHaveValue('+57 3000000000')
  })

  it('el PUT envía solo `correoCorporativo` y `telefono` (nunca el correo de acceso ni el RNT) y muestra lo guardado', async () => {
    const { solicitudes, usuario } = montar('/prestador/perfil', {
      [`GET ${PERFIL}`]: () => respuestaJson(200, perfilPrestador()),
      [`PUT ${PERFIL}`]: () => respuestaJson(200, perfilPrestador({ correoCorporativo: 'ventas@casatayrona.test', telefono: '+57 3111111111' })),
    })
    await screen.findByLabelText('Correo corporativo', {}, LENTO)
    fireEvent.change(screen.getByLabelText('Correo corporativo'), { target: { value: ' Ventas@CasaTayrona.test ' } })
    fireEvent.change(screen.getByLabelText('Teléfono'), { target: { value: '+57 3111111111' } })
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    expect(await screen.findByText('Perfil comercial actualizado.', {}, LENTO)).toHaveAttribute('role', 'status')
    expect(puts(solicitudes)).toHaveLength(1)
    // El servidor recorta y pasa a minúsculas (contrato); el input de correo ya quita los espacios.
    expect(puts(solicitudes)[0].cuerpo).toEqual({ correoCorporativo: 'Ventas@CasaTayrona.test', telefono: '+57 3111111111' })
    expect(Object.keys(puts(solicitudes)[0].cuerpo as object)).toEqual(['correoCorporativo', 'telefono'])
    expect(screen.getByLabelText('Correo corporativo')).toHaveValue('ventas@casatayrona.test')
  })

  it('validación en el cliente y 400 del servidor junto al campo', async () => {
    const { solicitudes, usuario } = montar('/prestador/perfil', {
      [`GET ${PERFIL}`]: () => respuestaJson(200, perfilPrestador()),
      [`PUT ${PERFIL}`]: () => respuestaProblema(400, 'DATOS_INVALIDOS', { campos: { telefono: 'LONGITUD_MAXIMA' } }),
    })
    await screen.findByLabelText('Correo corporativo', {}, LENTO)
    fireEvent.change(screen.getByLabelText('Correo corporativo'), { target: { value: 'sin-arroba' } })
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    expect(screen.getByLabelText('Correo corporativo')).toHaveAttribute('aria-invalid', 'true')
    expect(puts(solicitudes)).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Correo corporativo'), { target: { value: 'ventas@casatayrona.test' } })
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    await waitFor(() => expect(screen.getByLabelText('Teléfono')).toHaveAttribute('aria-invalid', 'true'))
    expect(screen.getByLabelText('Correo corporativo')).not.toHaveAttribute('aria-invalid')
  })

  it('límites de DEC-202 (C10): correo corporativo hasta 100 con forma de correo y teléfono hasta 20', () => {
    const correo100 = `${'a'.repeat(94)}@b.com`
    expect(correo100).toHaveLength(100)
    expect(validarPerfilComercial({ correoCorporativo: correo100, telefono: '1'.repeat(20) })).toEqual({})
    expect(validarPerfilComercial({ correoCorporativo: `a${correo100}`, telefono: '1'.repeat(21) })).toEqual({
      correoCorporativo: 'LONGITUD_MAXIMA',
      telefono: 'LONGITUD_MAXIMA',
    })
    expect(validarPerfilComercial({ correoCorporativo: 'ventas.casatayrona', telefono: '300' })).toEqual({ correoCorporativo: 'FORMATO' })
  })

  it('si el GET falla: mensaje y «Reintentar»', async () => {
    let llamadas = 0
    montar('/prestador/perfil', {
      [`GET ${PERFIL}`]: () => (++llamadas === 1 ? respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE') : respuestaJson(200, perfilPrestador())),
    })
    expect(await screen.findByText('No pudimos cargar tu perfil comercial.', {}, LENTO)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByLabelText('Correo corporativo', {}, LENTO)).toHaveValue('contacto@casatayrona.test')
  })
})
