import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import {
  DETALLE_HOTEL,
  USUARIO_TURISTA,
  disponibilidadAlojamiento,
  reservaConfirmada,
  respuestaJson,
  respuestaProblema,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp } from '../../../test/renderApp'
import { hoyBogota, sumarDias } from '../fechasBogota'

/**
 * Ajuste 3: la Idempotency-Key del intento en curso vive en sessionStorage, asociada al cuerpo exacto. Si la petición
 * se pierde y el turista recarga y confirma la misma selección, sale la MISMA clave (ms-booking no reserva dos veces).
 */
const HOY = hoyBogota()
const ENTRADA = sumarDias(HOY, 50)
const SALIDA = sumarDias(HOY, 53)
const RUTA = `/servicios/s-hotel/reservar?entrada=${ENTRADA}&salida=${SALIDA}&personas=5&unidad=u-suite&habitaciones=2`
const CLAVE_INTENTO = 'samaria360.intentoReserva'

const reservas = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.metodo === 'POST')
const resumen = () => screen.getByRole('complementary', { name: 'Resumen de reserva' })
const confirmar = () => within(resumen()).getByRole('button', { name: /Confirmar reserva|Confirmando/ })

function simular(reservar: () => Response | Promise<Response>) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  return simularFetch({
    'GET /api/v1/catalogo/servicios/s-hotel': () => respuestaJson(200, DETALLE_HOTEL),
    'GET /api/v1/reservas/disponibilidad': () =>
      respuestaJson(200, disponibilidadAlojamiento('s-hotel', ENTRADA, { 'u-doble': [2, 2, 2], 'u-suite': [3, 3, 3] })),
    'GET /api/v1/auth/me': () => respuestaJson(200, USUARIO_TURISTA),
    'POST /api/v1/reservas': reservar,
  })
}

async function confirmarCuandoSePueda(usuario: ReturnType<typeof renderApp>['usuario']) {
  await screen.findByRole('radio', { name: 'Suite familiar' })
  await waitFor(() => expect(confirmar()).not.toHaveAttribute('aria-disabled'))
  await usuario.click(confirmar())
}

describe('Idempotency-Key a prueba de recarga (D6, ajuste 3)', () => {
  it('error de red, recarga y confirmar: sale la misma clave', async () => {
    let intentos = 0
    const { solicitudes } = simular(() => {
      intentos += 1
      return intentos === 1 ? Promise.reject(new TypeError('Failed to fetch')) : respuestaJson(201, reservaConfirmada())
    })
    const primera = renderApp(RUTA)
    await confirmarCuandoSePueda(primera.usuario)
    await screen.findByRole('alert')
    // El intento quedó guardado: no fue una respuesta definitiva.
    expect(JSON.parse(sessionStorage.getItem(CLAVE_INTENTO)!)).toMatchObject({ idUsuario: USUARIO_TURISTA.idUsuario })

    // «Recarga»: se desmonta la app y se monta otra con la misma URL; sessionStorage sigue igual.
    primera.unmount()
    const segunda = renderApp(RUTA)
    await confirmarCuandoSePueda(segunda.usuario)
    await screen.findByRole('heading', { name: '¡Reserva confirmada!' })

    const [antes, despues] = reservas(solicitudes)
    expect(despues.cabeceras['Idempotency-Key']).toBe(antes.cabeceras['Idempotency-Key'])
    expect(despues.cuerpoTexto).toBe(antes.cuerpoTexto)
    expect(sessionStorage.getItem(CLAVE_INTENTO)).toBeNull()
  })

  it('una respuesta 4xx también es definitiva: borra el intento y el siguiente usa otra clave', async () => {
    let intentos = 0
    const { solicitudes } = simular(() => {
      intentos += 1
      return intentos === 1 ? respuestaProblema(409, 'CUPO_AGOTADO') : respuestaJson(201, reservaConfirmada())
    })
    const { usuario } = renderApp(RUTA)
    await confirmarCuandoSePueda(usuario)
    expect(await screen.findByRole('alert')).toHaveTextContent('Ya no quedan cupos suficientes')
    expect(sessionStorage.getItem(CLAVE_INTENTO)).toBeNull()

    await waitFor(() => expect(confirmar()).not.toHaveAttribute('aria-disabled'))
    await usuario.click(confirmar())
    await screen.findByRole('heading', { name: '¡Reserva confirmada!' })
    const [primero, segundo] = reservas(solicitudes)
    expect(segundo.cabeceras['Idempotency-Key']).not.toBe(primero.cabeceras['Idempotency-Key'])
  })

  it('si sessionStorage falla, el intento vive en memoria y «Reintentar» conserva la clave', async () => {
    const getItem = Storage.prototype.getItem
    const setItem = Storage.prototype.setItem
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(function (this: Storage, clave: string) {
      if (clave === CLAVE_INTENTO) throw new DOMException('Almacenamiento bloqueado', 'SecurityError')
      return getItem.call(this, clave)
    })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(function (this: Storage, clave: string, valor: string) {
      if (clave === CLAVE_INTENTO) throw new DOMException('Almacenamiento bloqueado', 'SecurityError')
      setItem.call(this, clave, valor)
    })
    let intentos = 0
    const { solicitudes } = simular(() => {
      intentos += 1
      return intentos === 1 ? Promise.reject(new TypeError('Failed to fetch')) : respuestaJson(201, reservaConfirmada())
    })
    const { usuario } = renderApp(RUTA)
    await confirmarCuandoSePueda(usuario)
    await usuario.click(within(await screen.findByRole('alert')).getByRole('button', { name: 'Reintentar' }))

    await screen.findByRole('heading', { name: '¡Reserva confirmada!' })
    const [primero, segundo] = reservas(solicitudes)
    expect(segundo.cabeceras['Idempotency-Key']).toBe(primero.cabeceras['Idempotency-Key'])
  })
})
