import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  PREGUNTA_ACCESIBLES,
  PREGUNTA_CANCELAR,
  USUARIO_ADMIN,
  USUARIO_PRESTADOR,
  USUARIO_TURISTA,
  listaElementos,
  respuestaJson,
  respuestaProblema,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

const ME = 'GET /api/v1/auth/me'
const FAQ = 'GET /api/v1/catalogo/faq'
const ADMIN_FAQ = '/api/v1/catalogo/admin/faq'
const RUTA = '/admin/preguntas-frecuentes'

type Manejador = (solicitud: SolicitudSimulada) => Response | Promise<Response>

/** Monta la gestión como Administrador; por defecto, dos preguntas. */
function montar(rutas: Record<string, Manejador> = {}) {
  sessionStorage.setItem('samaria360.token', 'token-admin')
  const simulado = simularFetch({
    [ME]: () => respuestaJson(200, USUARIO_ADMIN),
    [FAQ]: () => respuestaJson(200, listaElementos([PREGUNTA_ACCESIBLES, PREGUNTA_CANCELAR])),
    ...rutas,
  })
  return { ...simulado, ...renderApp(RUTA) }
}

const tabla = () => screen.findByRole('table')
const region = () => screen.getAllByRole('status').find((elemento) => elemento.getAttribute('aria-live') === 'polite')!
const listados = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.url === '/api/v1/catalogo/faq')

describe('WF-13, gestión de preguntas frecuentes (RF26, DEC-73)', () => {
  it('muestra «n de 200», la tabla con título, fecha de actualización y acciones, y usa el listado público sin token', async () => {
    const { solicitudes } = montar()

    const filas = within(await tabla()).getAllByRole('row').slice(1)
    expect(filas.map((fila) => within(fila).getAllByRole('cell')[0].textContent)).toEqual([
      PREGUNTA_ACCESIBLES.titulo,
      PREGUNTA_CANCELAR.titulo,
    ])
    expect(within(filas[0]).getByText('22/10/2026')).toBeInTheDocument()
    expect(within(filas[0]).getByRole('button', { name: `Editar: ${PREGUNTA_ACCESIBLES.titulo}` })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: '2 de 200 preguntas' })).toBeInTheDocument()
    expect(listados(solicitudes)[0].cabeceras.Authorization).toBeUndefined()
  })

  it('crear: los contadores cuentan, «Guardar» se habilita y el 201 avisa y recarga la lista', async () => {
    const nueva = { id: '6a1f000000000000000000aa', titulo: '¿Hay parqueadero?', cuerpo: 'Sí.', fechaActualizacion: '2026-10-23T13:00:00Z' }
    let creada = false
    const { usuario, solicitudes } = montar({
      [FAQ]: () =>
        respuestaJson(200, listaElementos(creada ? [nueva, PREGUNTA_ACCESIBLES, PREGUNTA_CANCELAR] : [PREGUNTA_ACCESIBLES, PREGUNTA_CANCELAR])),
      [`POST ${ADMIN_FAQ}`]: () => {
        creada = true
        return respuestaJson(201, nueva)
      },
    })
    await tabla()

    await usuario.click(screen.getByRole('button', { name: 'Nueva pregunta' }))
    const dialogo = screen.getByRole('dialog', { name: 'Nueva pregunta' })
    const guardar = within(dialogo).getByRole('button', { name: 'Guardar' })
    expect(guardar).toHaveAttribute('aria-disabled', 'true')
    const titulo = within(dialogo).getByLabelText('Título (obligatorio)')
    expect(titulo).toHaveFocus()

    await usuario.type(titulo, '  ¿Hay parqueadero?  ')
    expect(within(dialogo).getByText('17 de 150 caracteres')).toBeInTheDocument()
    await usuario.type(within(dialogo).getByLabelText('Cuerpo (obligatorio)'), 'Sí.')
    expect(within(dialogo).getByText('3 de 2000 caracteres')).toBeInTheDocument()
    expect(guardar).not.toHaveAttribute('aria-disabled')

    await usuario.click(guardar)

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(region()).toHaveTextContent('Se creó la pregunta «¿Hay parqueadero?».')
    const envio = solicitudes.find((s) => s.metodo === 'POST')!
    expect(envio.cuerpo).toEqual({ titulo: '¿Hay parqueadero?', cuerpo: 'Sí.' })
    expect(envio.cabeceras.Authorization).toBe('Bearer token-admin')
    // El orden lo da el servidor (DEC-63): se recarga en vez de insertar en el cliente.
    await waitFor(() => expect(listados(solicitudes)).toHaveLength(2))
    expect(await screen.findByRole('heading', { level: 2, name: '3 de 200 preguntas' })).toHaveFocus()
  })

  it('pasarse de 150 en el título se marca mientras se escribe y deshabilita «Guardar»', async () => {
    const { usuario } = montar()
    await tabla()
    await usuario.click(screen.getByRole('button', { name: 'Nueva pregunta' }))
    const dialogo = screen.getByRole('dialog')

    await usuario.click(within(dialogo).getByLabelText('Título (obligatorio)'))
    await usuario.paste('a'.repeat(151))
    await usuario.type(within(dialogo).getByLabelText('Cuerpo (obligatorio)'), 'x')

    expect(within(dialogo).getByText('151 de 150 caracteres')).toBeInTheDocument()
    expect(within(dialogo).getByRole('alert')).toHaveTextContent('Admite como máximo 150 caracteres.')
    expect(within(dialogo).getByLabelText('Título (obligatorio)')).toHaveAttribute('aria-invalid', 'true')
    expect(within(dialogo).getByRole('button', { name: 'Guardar' })).toHaveAttribute('aria-disabled', 'true')
  })

  it('editar: el formulario llega con los datos, el PUT responde 200 y se recarga', async () => {
    const { usuario, solicitudes } = montar({
      [`PUT ${ADMIN_FAQ}/${PREGUNTA_CANCELAR.id}`]: (solicitud) =>
        respuestaJson(200, { ...PREGUNTA_CANCELAR, ...(solicitud.cuerpo as object) }),
    })
    await tabla()

    await usuario.click(screen.getByRole('button', { name: `Editar: ${PREGUNTA_CANCELAR.titulo}` }))
    const dialogo = screen.getByRole('dialog', { name: 'Editar pregunta' })
    const cuerpo = within(dialogo).getByLabelText('Cuerpo (obligatorio)')
    expect(cuerpo).toHaveValue(PREGUNTA_CANCELAR.cuerpo)
    await usuario.clear(cuerpo)
    await usuario.type(cuerpo, 'Hasta 24 horas antes, límite incluido.')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(region()).toHaveTextContent(`Se guardaron los cambios de «${PREGUNTA_CANCELAR.titulo}».`)
    expect(solicitudes.find((s) => s.metodo === 'PUT')!.cuerpo).toEqual({
      titulo: PREGUNTA_CANCELAR.titulo,
      cuerpo: 'Hasta 24 horas antes, límite incluido.',
    })
    await waitFor(() => expect(listados(solicitudes)).toHaveLength(2))
  })

  it('eliminar pide confirmación (foco en «Cancelar»), el 204 avisa y recarga', async () => {
    const { usuario, solicitudes } = montar({
      [`DELETE ${ADMIN_FAQ}/${PREGUNTA_CANCELAR.id}`]: () => new Response(null, { status: 204 }),
    })
    await tabla()

    await usuario.click(screen.getByRole('button', { name: `Eliminar: ${PREGUNTA_CANCELAR.titulo}` }))
    const dialogo = screen.getByRole('dialog', { name: `Eliminar «${PREGUNTA_CANCELAR.titulo}»` })
    expect(dialogo).toHaveTextContent('No se puede deshacer.')
    expect(within(dialogo).getByRole('button', { name: 'Cancelar' })).toHaveFocus()
    await usuario.click(within(dialogo).getByRole('button', { name: 'Eliminar' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(region()).toHaveTextContent(`Se eliminó la pregunta «${PREGUNTA_CANCELAR.titulo}».`)
    expect(solicitudes.filter((s) => s.metodo === 'DELETE')).toHaveLength(1)
    await waitFor(() => expect(listados(solicitudes)).toHaveLength(2))
  })

  it('un 400 DATOS_INVALIDOS muestra el código de cada campo en su campo y deja el diálogo abierto', async () => {
    const { usuario } = montar({
      [`POST ${ADMIN_FAQ}`]: () =>
        respuestaProblema(400, 'DATOS_INVALIDOS', { campos: { titulo: 'REQUERIDO', cuerpo: 'LONGITUD_MAXIMA' } }),
    })
    await tabla()
    await usuario.click(screen.getByRole('button', { name: 'Nueva pregunta' }))
    const dialogo = screen.getByRole('dialog')
    await usuario.type(within(dialogo).getByLabelText('Título (obligatorio)'), 'x')
    await usuario.type(within(dialogo).getByLabelText('Cuerpo (obligatorio)'), 'y')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }))

    const titulo = within(dialogo).getByLabelText('Título (obligatorio)')
    await waitFor(() => expect(titulo).toHaveAttribute('aria-invalid', 'true'))
    expect(titulo).toHaveAccessibleDescription(/Este campo es obligatorio\./)
    expect(within(dialogo).getByLabelText('Cuerpo (obligatorio)')).toHaveAccessibleDescription(
      /Admite como máximo 2000 caracteres\./,
    )
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('un 404 FAQ_NO_ENCONTRADA (otra sesión la borró) cierra el diálogo, avisa y recarga', async () => {
    const { usuario, solicitudes } = montar({
      [`DELETE ${ADMIN_FAQ}/${PREGUNTA_CANCELAR.id}`]: () => respuestaProblema(404, 'FAQ_NO_ENCONTRADA'),
    })
    await tabla()
    await usuario.click(screen.getByRole('button', { name: `Eliminar: ${PREGUNTA_CANCELAR.titulo}` }))
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Eliminar' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(region()).toHaveTextContent('La pregunta ya no existe; se recargó la lista.')
    await waitFor(() => expect(listados(solicitudes)).toHaveLength(2))
  })

  it('un 409 LIMITE_FAQ avisa y recarga; con 200 preguntas, «Nueva pregunta» queda deshabilitada con su motivo', async () => {
    const llena = Array.from({ length: 200 }, (_, i) => ({ ...PREGUNTA_ACCESIBLES, id: `p${i}`, titulo: `Pregunta ${i}` }))
    let limite = false
    const { usuario, solicitudes } = montar({
      [FAQ]: () => respuestaJson(200, listaElementos(limite ? llena : [PREGUNTA_ACCESIBLES])),
      [`POST ${ADMIN_FAQ}`]: () => {
        limite = true
        return respuestaProblema(409, 'LIMITE_FAQ')
      },
    })
    await tabla()
    await usuario.click(screen.getByRole('button', { name: 'Nueva pregunta' }))
    const dialogo = screen.getByRole('dialog')
    await usuario.type(within(dialogo).getByLabelText('Título (obligatorio)'), 'x')
    await usuario.type(within(dialogo).getByLabelText('Cuerpo (obligatorio)'), 'y')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(region()).toHaveTextContent('Ya hay 200 preguntas, el máximo permitido; se recargó la lista.')
    await waitFor(() => expect(listados(solicitudes)).toHaveLength(2))
    expect(await screen.findByRole('heading', { level: 2, name: '200 de 200 preguntas' })).toBeInTheDocument()

    const nueva = screen.getByRole('button', { name: 'Nueva pregunta' })
    expect(nueva).toHaveAttribute('aria-disabled', 'true')
    expect(nueva).toHaveAccessibleDescription('Ya hay 200 preguntas, el máximo permitido. Elimina una para crear otra.')
    await usuario.click(nueva)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})

describe('Seguridad de la gestión de preguntas (RNF06, DEC-51)', () => {
  it.each([USUARIO_TURISTA, USUARIO_PRESTADOR])('$rol ve «Sin permiso» y no se pide la lista', async (usuarioActual) => {
    sessionStorage.setItem('samaria360.token', 'token-otro')
    const { solicitudes } = simularFetch({ [ME]: () => respuestaJson(200, usuarioActual) })
    renderApp(RUTA)

    expect(await screen.findByRole('heading', { level: 1, name: 'Sin permiso' })).toBeInTheDocument()
    expect(listados(solicitudes)).toHaveLength(0)
  })

  it('sin sesión lleva al login', async () => {
    simularFetch({})
    renderApp(RUTA)

    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
  })

  it('un 401 SESION_INVALIDADA al guardar cierra la sesión y el login dice por qué (DEC-49, DEC-50)', async () => {
    const { usuario } = montar({ [`DELETE ${ADMIN_FAQ}/${PREGUNTA_CANCELAR.id}`]: () => respuestaProblema(401, 'SESION_INVALIDADA') })
    await tabla()
    await usuario.click(screen.getByRole('button', { name: `Eliminar: ${PREGUNTA_CANCELAR.titulo}` }))
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Eliminar' }))

    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
    expect(
      await screen.findByText('Tu sesión se cerró porque cambió el estado o los permisos de tu cuenta.'),
    ).toBeInTheDocument()
    expect(sessionStorage.getItem('samaria360.token')).toBeNull()
  })
})
