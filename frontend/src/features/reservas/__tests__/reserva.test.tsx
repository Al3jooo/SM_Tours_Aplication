import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  DETALLE_HOTEL,
  DETALLE_RESTAURANTE,
  DETALLE_TOUR,
  SESION_TURISTA,
  USUARIO_PRESTADOR,
  USUARIO_TURISTA,
  disponibilidadAlojamiento,
  disponibilidadTour,
  paginaOferta,
  reservaConfirmada,
  respuestaJson,
  respuestaProblema,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'
import { formatearFecha, hoyBogota, sumarDias } from '../fechasBogota'

const HOY = hoyBogota()
// Al final de la ventana, como la reserva de prueba del recorrido: 3 noches.
const ENTRADA = sumarDias(HOY, 50)
const SALIDA = sumarDias(HOY, 53)
const DIA_TOUR = sumarDias(HOY, 51)

const DETALLE = 'GET /api/v1/catalogo/servicios'
const DISPONIBILIDAD = 'GET /api/v1/reservas/disponibilidad'
const RESERVAR = 'POST /api/v1/reservas'
const ME = 'GET /api/v1/auth/me'

/** Doble: sin cupo la segunda noche. Suite: cupo mínimo 2 (3, 2, 3). */
const CUPOS_HOTEL = { 'u-doble': [2, 0, 1], 'u-suite': [3, 2, 3] }
const RUTA_HOTEL = `/servicios/s-hotel/reservar?entrada=${ENTRADA}&salida=${SALIDA}&personas=5`
const RUTA_SUITE = `${RUTA_HOTEL}&unidad=u-suite&habitaciones=2`
const RUTA_TOUR = `/servicios/s-tour-cafe/reservar?fecha=${DIA_TOUR}&personas=2`

const iniciarSesion = () => sessionStorage.setItem('samaria360.token', 'token-guardado')
const reservas = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.metodo === 'POST' && s.url === '/api/v1/reservas')
const consultasDe = (solicitudes: SolicitudSimulada[], prefijo: string) =>
  solicitudes.filter((s) => s.metodo === 'GET' && s.url.startsWith(prefijo))

function simularHotel(extra: Parameters<typeof simularFetch>[0] = {}, usuario: object = USUARIO_TURISTA) {
  return simularFetch({
    [`${DETALLE}/s-hotel`]: () => respuestaJson(200, DETALLE_HOTEL),
    [DISPONIBILIDAD]: () => respuestaJson(200, disponibilidadAlojamiento('s-hotel', ENTRADA, CUPOS_HOTEL)),
    [ME]: () => respuestaJson(200, usuario),
    ...extra,
  })
}

const resumen = () => screen.getByRole('complementary', { name: 'Resumen de reserva' })
const confirmar = () => within(resumen()).getByRole('button', { name: /Confirmar reserva|Confirmando/ })

describe('WF-10, alojamiento: unidades, cupos y resumen', () => {
  it('con las fechas en la URL consulta al cargar; el cupo es el mínimo del rango y se elige con radios', async () => {
    const { solicitudes } = simularHotel()
    const { usuario } = renderApp(RUTA_HOTEL)

    expect(await screen.findByRole('heading', { level: 1, name: 'Hotel Casa Tayrona – Disponibilidad y reserva' })).toBeInTheDocument()
    const grupo = await screen.findByRole('group', { name: 'Elige la habitación' })
    const consulta = new URL(consultasDe(solicitudes, '/api/v1/reservas/disponibilidad')[0].url, 'http://x').searchParams
    expect(consulta.get('desde')).toBe(ENTRADA)
    expect(consulta.get('hasta')).toBe(sumarDias(SALIDA, -1))

    // La Doble no tiene cupo la segunda noche: radio deshabilitado y el motivo en texto.
    const doble = within(grupo).getByRole('radio', { name: 'Doble' })
    expect(doble).toBeDisabled()
    expect(doble).toHaveAccessibleDescription(
      expect.stringContaining(`Sin cupo el ${formatearFecha(sumarDias(ENTRADA, 1), 'es')}.`),
    )
    // La Suite: el mínimo de las tres noches (3, 2, 3).
    const suite = within(grupo).getByRole('radio', { name: 'Suite familiar' })
    expect(suite).toBeEnabled()
    expect(suite).toHaveAccessibleDescription(expect.stringContaining('Quedan 2 habitaciones para todas tus noches.'))

    await usuario.click(suite)
    expect(ubicacionActual()).toContain('unidad=u-suite')
    expect(suite).toBeChecked()
  })

  it('el resumen muestra el total en centavos exactos antes de confirmar y pide habitaciones suficientes', async () => {
    simularHotel()
    const { usuario } = renderApp(`${RUTA_HOTEL}&unidad=u-suite`)

    await screen.findByRole('radio', { name: 'Suite familiar' })
    // 5 huéspedes no caben en 1 Suite (4 por habitación).
    const habitaciones = screen.getByRole('spinbutton', { name: 'Habitaciones' })
    expect(habitaciones).toHaveAccessibleDescription(expect.stringContaining('necesitas más habitaciones'))
    expect(confirmar()).toHaveAttribute('aria-disabled', 'true')
    expect(confirmar()).toHaveAccessibleDescription('Revisa los datos marcados.')

    await usuario.clear(habitaciones)
    await usuario.type(habitaciones, '2')
    // 260.000,50 × 3 noches × 2 habitaciones = 1.560.003 (sin centavos: sin decimales).
    expect(within(resumen()).getByText(/260\.000,50 × 3 noches × 2 habitaciones/)).toBeInTheDocument()
    expect(within(resumen()).getByText('Total').nextElementSibling).toHaveTextContent(/1\.560\.003$/)
    expect(within(resumen()).getByText('Suite familiar')).toBeInTheDocument()
    expect(confirmar()).not.toHaveAttribute('aria-disabled')
    // Información importante: lo que envía ms-booking.
    expect(within(resumen()).getByText('Check-in desde las 15:00.')).toBeInTheDocument()
    expect(within(resumen()).getByText('Check-out hasta las 12:00.')).toBeInTheDocument()
    expect(within(resumen()).getByText('Cancelación gratuita hasta 24 horas antes del inicio.')).toBeInTheDocument()

    // Más habitaciones que el cupo mostrado.
    await usuario.clear(habitaciones)
    await usuario.type(habitaciones, '3')
    expect(habitaciones).toHaveAccessibleDescription(expect.stringContaining('Solo quedan 2.'))
  })

  it('validación: la fecha pasada y la estadía de más de 14 noches no consultan', async () => {
    const { solicitudes } = simularHotel()
    const { usuario } = renderApp('/servicios/s-hotel/reservar')

    const entrada = await screen.findByLabelText('Entrada (check-in)')
    fireEvent.change(entrada, { target: { value: sumarDias(HOY, -1) } })
    fireEvent.change(screen.getByLabelText('Salida (check-out)'), { target: { value: sumarDias(HOY, 20) } })
    await usuario.click(screen.getByRole('button', { name: 'Verificar disponibilidad en tiempo real' }))

    expect(entrada).toHaveAccessibleDescription(expect.stringContaining('La fecha ya pasó.'))
    expect(entrada).toHaveFocus()
    expect(consultasDe(solicitudes, '/api/v1/reservas/disponibilidad')).toHaveLength(0)

    fireEvent.change(entrada, { target: { value: sumarDias(HOY, 1) } })
    // La entrada nueva ajustó la salida a entrada + 1; se escribe a mano una de 19 noches.
    fireEvent.change(screen.getByLabelText('Salida (check-out)'), { target: { value: sumarDias(HOY, 20) } })
    await usuario.click(screen.getByRole('button', { name: 'Verificar disponibilidad en tiempo real' }))
    expect(screen.getByLabelText('Salida (check-out)')).toHaveAccessibleDescription(
      expect.stringContaining('14 noches como máximo'),
    )
    expect(consultasDe(solicitudes, '/api/v1/reservas/disponibilidad')).toHaveLength(0)
  })

  it('al mover la entrada después de la salida, la salida se ajusta y se anuncia', async () => {
    simularHotel()
    renderApp(RUTA_HOTEL)

    const entrada = await screen.findByLabelText('Entrada (check-in)')
    fireEvent.change(entrada, { target: { value: sumarDias(SALIDA, 2) } })

    expect(screen.getByLabelText('Salida (check-out)')).toHaveValue(sumarDias(SALIDA, 3))
    expect(screen.getByText(`Ajustamos la salida al ${formatearFecha(sumarDias(SALIDA, 3), 'es')} para que la estadía sea válida.`)).toHaveAttribute('role', 'status')
    // Las fechas cambiaron: hay que verificar de nuevo antes de confirmar.
    expect(confirmar()).toHaveAccessibleDescription('Cambiaste las fechas: verifica la disponibilidad de nuevo.')
  })

  it('un precio publicado con formato inválido deja el total sin calcular y «Confirmar» bloqueado', async () => {
    const suiteInvalida = { ...DETALLE_HOTEL.detalleCategoria.unidades[1], precioNoche: '260000.123' }
    simularHotel({
      [`${DETALLE}/s-hotel`]: () =>
        respuestaJson(200, {
          ...DETALLE_HOTEL,
          detalleCategoria: { ...DETALLE_HOTEL.detalleCategoria, unidades: [DETALLE_HOTEL.detalleCategoria.unidades[0], suiteInvalida] },
        }),
    })
    renderApp(RUTA_SUITE)

    await screen.findByRole('radio', { name: 'Suite familiar' })
    expect(within(resumen()).queryByText('Total')).not.toBeInTheDocument()
    expect(confirmar()).toHaveAttribute('aria-disabled', 'true')
    expect(confirmar()).toHaveAccessibleDescription(expect.stringContaining('No pudimos calcular el total'))
  })
})

describe('WF-10, confirmar (RF20) con Idempotency-Key', () => {
  it('envía precioTotalEsperado como número exacto y la clave; el 201 muestra la confirmación', async () => {
    iniciarSesion()
    const { solicitudes } = simularHotel({ [RESERVAR]: () => respuestaJson(201, reservaConfirmada()) })
    const { usuario } = renderApp(RUTA_SUITE)

    await screen.findByRole('radio', { name: 'Suite familiar' })
    await waitFor(() => expect(confirmar()).not.toHaveAttribute('aria-disabled'))
    await usuario.click(confirmar())

    const titulo = await screen.findByRole('heading', { name: '¡Reserva confirmada!' })
    expect(titulo).toHaveFocus()
    expect(screen.getByRole('link', { name: 'Ver mis reservas' })).toHaveAttribute('href', '/mis-reservas')

    const [envio] = reservas(solicitudes)
    expect(envio.cuerpoTexto).toContain('"precioTotalEsperado":1560003.00')
    expect(envio.cuerpo).toEqual({
      idServicio: 's-hotel',
      idUnidad: 'u-suite',
      fechaEntrada: ENTRADA,
      fechaSalida: SALIDA,
      cantidad: 2,
      personas: 5,
      precioTotalEsperado: 1560003,
    })
    expect(envio.cabeceras['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(envio.cabeceras.Authorization).toBe('Bearer token-guardado')
    // Respuesta definitiva: el intento se borra.
    expect(sessionStorage.getItem('samaria360.intentoReserva')).toBeNull()
  })

  it('PRECIO_CAMBIADO muestra los dos precios y, al aceptarlo, envía el nuevo total con otra clave', async () => {
    iniciarSesion()
    let intentos = 0
    const { solicitudes } = simularHotel({
      [RESERVAR]: () => {
        intentos += 1
        return intentos === 1
          ? respuestaProblema(409, 'PRECIO_CAMBIADO', { precioTotal: 1680000, precioUnitario: 280000, moneda: 'COP' })
          : respuestaJson(201, reservaConfirmada({ precioTotal: 1680000 }))
      },
    })
    const { usuario } = renderApp(RUTA_SUITE)

    await screen.findByRole('radio', { name: 'Suite familiar' })
    await waitFor(() => expect(confirmar()).not.toHaveAttribute('aria-disabled'))
    await usuario.click(confirmar())

    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent(/antes \$\s1\.560\.003, ahora \$\s1\.680\.000/)
    expect(alerta).toHaveFocus()
    await usuario.click(within(alerta).getByRole('button', { name: 'Aceptar el nuevo precio y confirmar' }))

    await screen.findByRole('heading', { name: '¡Reserva confirmada!' })
    const [primero, segundo] = reservas(solicitudes)
    expect(segundo.cuerpoTexto).toContain('"precioTotalEsperado":1680000.00')
    expect(segundo.cabeceras['Idempotency-Key']).not.toBe(primero.cabeceras['Idempotency-Key'])
  })

  it.each([
    ['un error de red', () => Promise.reject(new TypeError('Failed to fetch'))],
    ['un 503', () => respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE', { servicio: 'ms-booking' })],
  ])('después de %s, «Reintentar» envía la misma clave', async (_caso, fallo) => {
    iniciarSesion()
    let intentos = 0
    const { solicitudes } = simularHotel({
      [RESERVAR]: () => {
        intentos += 1
        return intentos === 1 ? fallo() : respuestaJson(201, reservaConfirmada())
      },
    })
    const { usuario } = renderApp(RUTA_SUITE)

    await screen.findByRole('radio', { name: 'Suite familiar' })
    await waitFor(() => expect(confirmar()).not.toHaveAttribute('aria-disabled'))
    await usuario.click(confirmar())
    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent('no se reservará dos veces')
    await usuario.click(within(alerta).getByRole('button', { name: 'Reintentar' }))

    await screen.findByRole('heading', { name: '¡Reserva confirmada!' })
    const [primero, segundo] = reservas(solicitudes)
    expect(segundo.cabeceras['Idempotency-Key']).toBe(primero.cabeceras['Idempotency-Key'])
    expect(segundo.cuerpoTexto).toBe(primero.cuerpoTexto)
  })

  it('si la selección cambia después de un error, la clave es nueva', async () => {
    iniciarSesion()
    const { solicitudes } = simularHotel({ [RESERVAR]: () => Promise.reject(new TypeError('Failed to fetch')) })
    const { usuario } = renderApp(RUTA_SUITE)

    await screen.findByRole('radio', { name: 'Suite familiar' })
    await waitFor(() => expect(confirmar()).not.toHaveAttribute('aria-disabled'))
    await usuario.click(confirmar())
    await screen.findByRole('alert')

    const huespedes = screen.getByRole('spinbutton', { name: 'Huéspedes' })
    await usuario.clear(huespedes)
    await usuario.type(huespedes, '6')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    await usuario.click(confirmar())
    await screen.findByRole('alert')

    const [primero, segundo] = reservas(solicitudes)
    expect(segundo.cuerpo).toMatchObject({ personas: 6 })
    expect(segundo.cabeceras['Idempotency-Key']).not.toBe(primero.cabeceras['Idempotency-Key'])
  })

  it.each([
    ['CUPO_AGOTADO', 'Ya no quedan cupos suficientes', false],
    ['FECHA_NO_DISPONIBLE', 'ya no tiene inventario publicado', false],
    ['UNIDAD_NO_DISPONIBLE', 'Esa habitación ya no está disponible', true],
  ])('%s muestra su mensaje y vuelve a consultar la disponibilidad', async (codigo, mensaje, recargaDetalle) => {
    iniciarSesion()
    const { solicitudes } = simularHotel({ [RESERVAR]: () => respuestaProblema(409, codigo) })
    const { usuario } = renderApp(RUTA_SUITE)

    await screen.findByRole('radio', { name: 'Suite familiar' })
    await waitFor(() => expect(confirmar()).not.toHaveAttribute('aria-disabled'))
    const disponibilidadAntes = consultasDe(solicitudes, '/api/v1/reservas/disponibilidad').length
    const detalleAntes = consultasDe(solicitudes, '/api/v1/catalogo/servicios').length
    await usuario.click(confirmar())

    expect(await screen.findByRole('alert')).toHaveTextContent(mensaje)
    await waitFor(() =>
      expect(consultasDe(solicitudes, '/api/v1/reservas/disponibilidad').length).toBe(disponibilidadAntes + 1),
    )
    expect(consultasDe(solicitudes, '/api/v1/catalogo/servicios').length).toBe(detalleAntes + (recargaDetalle ? 1 : 0))
  })

  it('sin sesión, «Confirmar» lleva al login y vuelve a WF-10 con la misma selección', async () => {
    simularHotel({
      'POST /api/v1/auth/login': () => respuestaJson(200, SESION_TURISTA),
      'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([])),
    })
    const { usuario } = renderApp(RUTA_SUITE)

    await screen.findByRole('radio', { name: 'Suite familiar' })
    await waitFor(() => expect(confirmar()).not.toHaveAttribute('aria-disabled'))
    await usuario.click(confirmar())

    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
    await usuario.type(await screen.findByLabelText('Correo electrónico'), 'turista@samaria360.test')
    await usuario.type(screen.getByLabelText('Contraseña'), 'Samaria360*')
    await usuario.click(screen.getByRole('button', { name: /Iniciar sesión/ }))

    await waitFor(() => expect(ubicacionActual()).toBe(RUTA_SUITE))
    expect(await screen.findByRole('radio', { name: 'Suite familiar' })).toBeChecked()
    expect(screen.getByRole('spinbutton', { name: 'Habitaciones' })).toHaveValue(2)
  }, 15_000)

  it('con el rol Prestador avisa que solo los turistas reservan y no envía nada', async () => {
    iniciarSesion()
    const { solicitudes } = simularHotel({}, USUARIO_PRESTADOR)
    const { usuario } = renderApp(RUTA_SUITE)

    await screen.findByRole('radio', { name: 'Suite familiar' })
    await waitFor(() => expect(confirmar()).not.toHaveAttribute('aria-disabled'))
    // Que la sesión del prestador ya esté confirmada.
    await screen.findByRole('button', { name: /Menú de usuario/ })
    await usuario.click(confirmar())

    expect(await screen.findByRole('alert')).toHaveTextContent('Solo los turistas pueden reservar.')
    expect(reservas(solicitudes)).toHaveLength(0)
  })
})

describe('WF-10, tour', () => {
  it('los horarios son radios con su cupo; el agotado queda deshabilitado con «Agotado»', async () => {
    iniciarSesion()
    const { solicitudes } = simularFetch({
      [`${DETALLE}/s-tour-cafe`]: () => respuestaJson(200, DETALLE_TOUR),
      [DISPONIBILIDAD]: () => respuestaJson(200, disponibilidadTour('s-tour-cafe', DIA_TOUR, { '08:00:00': 5, '14:00:00': 0 })),
      [ME]: () => respuestaJson(200, USUARIO_TURISTA),
      [RESERVAR]: () => respuestaJson(201, reservaConfirmada({ categoria: 'Tour', horaInicio: '08:00:00' })),
    })
    const { usuario } = renderApp(RUTA_TOUR)

    const horarios = await screen.findByRole('group', { name: 'Horario' })
    expect(within(horarios).getByRole('radio', { name: '08:00 · quedan 5 cupos' })).toBeEnabled()
    expect(within(horarios).getByRole('radio', { name: '14:00 · Agotado' })).toBeDisabled()
    // Información importante del tour: sin check-in ni check-out.
    expect(within(resumen()).queryByText(/Check-in/)).not.toBeInTheDocument()

    await usuario.click(within(horarios).getByRole('radio', { name: '08:00 · quedan 5 cupos' }))
    expect(ubicacionActual()).toContain('hora=08%3A00')
    expect(within(resumen()).getByText(/80\.000 × 2 personas/)).toBeInTheDocument()
    await waitFor(() => expect(confirmar()).not.toHaveAttribute('aria-disabled'))
    await usuario.click(confirmar())

    await screen.findByRole('heading', { name: '¡Reserva confirmada!' })
    expect(reservas(solicitudes)[0].cuerpoTexto).toBe(
      `{"idServicio":"s-tour-cafe","fechaEntrada":"${DIA_TOUR}","horaInicio":"08:00","personas":2,"precioTotalEsperado":160000.00}`,
    )
  })

  it('más personas que el cupo del horario no se pueden confirmar', async () => {
    simularFetch({
      [`${DETALLE}/s-tour-cafe`]: () => respuestaJson(200, DETALLE_TOUR),
      [DISPONIBILIDAD]: () => respuestaJson(200, disponibilidadTour('s-tour-cafe', DIA_TOUR, { '08:00:00': 1 })),
    })
    renderApp(`${RUTA_TOUR}&hora=08:00`)

    await screen.findByRole('radio', { name: '08:00 · queda 1 cupo' })
    expect(screen.getByRole('spinbutton', { name: 'Personas' })).toHaveAccessibleDescription(
      expect.stringContaining('Solo quedan 1.'),
    )
    expect(confirmar()).toHaveAttribute('aria-disabled', 'true')
  })

  it('un servicio que no se reserva (Gastronomía) lo dice en WF-10', async () => {
    simularFetch({ [`${DETALLE}/s-restaurante`]: () => respuestaJson(200, DETALLE_RESTAURANTE) })
    renderApp('/servicios/s-restaurante/reservar')

    expect(await screen.findByText(/Este servicio no se reserva en línea/)).toBeInTheDocument()
  })
})
