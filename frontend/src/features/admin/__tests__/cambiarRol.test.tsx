import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import i18n from '../../../i18n'
import {
  PERSONAL_ADMIN,
  PERSONAL_ADMIN_PROPIO,
  PERSONAL_GESTOR,
  PERSONAL_GESTOR_SEMILLA,
  TURISTA_ACTIVO,
  TURISTA_DESACTIVADO,
  USUARIO_ADMIN,
  USUARIO_GESTOR,
  USUARIO_PRESTADOR,
  USUARIO_TURISTA,
  paginaAdmin,
  respuestaJson,
  respuestaProblema,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

/** «Cambiar rol de cuenta» (RF45) en WF-12: pestaña «Personal», «Asignar rol» en Turistas y diálogo (DEC-115 a DEC-123). */

const ME = 'GET /api/v1/auth/me'
const BASE = '/api/v1/auth/admin'
const PERSONAL = `GET ${BASE}/personal`
const TURISTAS = `GET ${BASE}/turistas`
const PRESTADORES = `GET ${BASE}/prestadores`
const RUTA_PERSONAL = '/admin/cuentas?pestana=personal'
const RUTA_TURISTAS = '/admin/cuentas?pestana=turistas'

const cambioRol = (idUsuario: string) => `POST ${BASE}/cuentas/${idUsuario}/cambio-rol`

type Manejador = (solicitud: SolicitudSimulada) => Response | Promise<Response>

const consulta = (solicitud: SolicitudSimulada) => new URL(solicitud.url, 'http://localhost').searchParams

/** Las consultas a un listado del Administrador. */
function listados(solicitudes: SolicitudSimulada[], ruta: string) {
  return solicitudes.filter((s) => s.metodo === 'GET' && s.url.startsWith(`${BASE}/${ruta}?`))
}

const cambios = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.url.endsWith('/cambio-rol'))

/** Monta /admin/cuentas como Administrador (USUARIO_ADMIN); por defecto, «Personal» con las cuatro filas de prueba. */
function montar(ruta: string, rutas: Record<string, Manejador> = {}) {
  sessionStorage.setItem('samaria360.token', 'token-admin')
  const simulado = simularFetch({
    [ME]: () => respuestaJson(200, USUARIO_ADMIN),
    // El contador de «Prestadores por validar».
    [PRESTADORES]: () => respuestaJson(200, paginaAdmin([])),
    [PERSONAL]: () =>
      respuestaJson(200, paginaAdmin([PERSONAL_GESTOR, PERSONAL_GESTOR_SEMILLA, PERSONAL_ADMIN, PERSONAL_ADMIN_PROPIO])),
    ...rutas,
  })
  return { ...simulado, ...renderApp(ruta) }
}

const tabla = () => screen.findByRole('table')
const dialogo = () => screen.getByRole('dialog')
const titulo = () => screen.getByRole('heading', { level: 2, name: /cuenta/ })

/** El anuncio está una sola vez en la página, dentro de la región `aria-live` de WF-12. */
async function anunciado(texto: string) {
  const aviso = await screen.findByText(texto)
  expect(aviso.closest('[aria-live="polite"]')).not.toBeNull()
  expect(screen.getAllByText(texto)).toHaveLength(1)
}

/** Fila de la tabla que contiene el texto. */
function fila(texto: string) {
  const celda = within(screen.getByRole('table')).getByText(texto)
  return celda.closest('tr') as HTMLElement
}

describe('Acceso (RNF06, DEC-51)', () => {
  it.each([
    ['Turista', USUARIO_TURISTA],
    ['Prestador', USUARIO_PRESTADOR],
    ['Gestor', USUARIO_GESTOR],
  ])('un %s en «Personal» ve «Sin permiso» y no se llama a la API de administración', async (_rol, usuario) => {
    sessionStorage.setItem('samaria360.token', 'token-otro')
    const { solicitudes } = simularFetch({ [ME]: () => respuestaJson(200, usuario) })
    renderApp(RUTA_PERSONAL)

    expect(await screen.findByRole('heading', { level: 1, name: 'Sin permiso' })).toBeInTheDocument()
    expect(solicitudes.some((s) => s.url.startsWith(BASE))).toBe(false)
  })

  it('sin sesión, «Personal» lleva al login', async () => {
    renderApp(RUTA_PERSONAL)
    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
  })

  it('todas las llamadas de «Personal» y del cambio de rol llevan el token', async () => {
    const { usuario, solicitudes } = montar(RUTA_PERSONAL, {
      [cambioRol(PERSONAL_GESTOR.idUsuario)]: () => respuestaJson(200, { ...PERSONAL_GESTOR, rol: 'Administrador' }),
    })
    await usuario.click(await screen.findByRole('button', { name: 'Pasar a Administrador: Luis Gómez' }))
    await usuario.click(within(dialogo()).getByRole('button', { name: 'Pasar a Administrador' }))
    await anunciado('Luis Gómez ahora es Administrador.')

    const deAdministracion = solicitudes.filter((s) => s.url.startsWith(BASE))
    expect(deAdministracion.length).toBeGreaterThanOrEqual(3)
    expect(deAdministracion.every((s) => s.cabeceras.Authorization === 'Bearer token-admin')).toBe(true)
  })
})

describe('«Personal»: Consultar personal (RF45, DEC-115)', () => {
  it('carga la página 1 con nombre (o «—»), correo, rol, fecha de registro y acción', async () => {
    const { solicitudes } = montar(RUTA_PERSONAL)

    const filas = within(await tabla()).getAllByRole('row')
    expect(screen.getByRole('tab', { name: 'Personal' })).toHaveAttribute('aria-selected', 'true')
    expect(within(filas[0]).getAllByRole('columnheader').map((c) => c.textContent)).toEqual([
      'Nombre',
      'Correo',
      'Rol',
      'Registrado',
      'Acción',
    ])
    expect(within(filas[1]).getAllByRole('cell').slice(0, 4).map((c) => c.textContent)).toEqual([
      'Luis Gómez',
      'luis@mail.com',
      'Gestor',
      '18/10/2026',
    ])
    expect(within(filas[2]).getAllByRole('cell')[0]).toHaveTextContent('—')
    expect(within(filas[3]).getAllByRole('cell')[2]).toHaveTextContent('Administrador')
    const pedida = consulta(listados(solicitudes, 'personal')[0])
    expect([pedida.get('pagina'), pedida.get('rol'), pedida.get('q')]).toEqual(['0', null, null])
  })

  it('el filtro de rol y la búsqueda cambian la URL y la consulta', async () => {
    const { usuario, solicitudes } = montar(RUTA_PERSONAL)
    await tabla()

    await usuario.selectOptions(screen.getByLabelText('Rol'), 'Administrador')
    await waitFor(() => expect(ubicacionActual()).toBe('/admin/cuentas?pestana=personal&rol=Administrador'))
    await usuario.type(screen.getByLabelText('Buscar'), 'ana')
    await usuario.click(screen.getByRole('button', { name: 'Buscar' }))
    await waitFor(() => expect(ubicacionActual()).toBe('/admin/cuentas?pestana=personal&rol=Administrador&q=ana'))

    await waitFor(() => expect(listados(solicitudes, 'personal')).toHaveLength(3))
    const ultima = consulta(listados(solicitudes, 'personal').at(-1)!)
    expect([ultima.get('rol'), ultima.get('q'), ultima.get('pagina')]).toEqual(['Administrador', 'ana', '0'])
    expect(screen.getByLabelText('Buscar')).toHaveAttribute('placeholder', 'Nombre o correo')
  })

  it('páginas de 10: «Siguiente» pide pagina=1 a la API y escribe pagina=2 en la URL', async () => {
    const { usuario, solicitudes } = montar(RUTA_PERSONAL, {
      [PERSONAL]: (s) =>
        respuestaJson(200, paginaAdmin([PERSONAL_GESTOR], { pagina: Number(consulta(s).get('pagina')), totalElementos: 25 })),
    })
    expect(await screen.findByText('Página 1 de 3')).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: /Siguiente/ }))
    expect(await screen.findByText('Página 2 de 3')).toBeInTheDocument()
    expect(ubicacionActual()).toBe('/admin/cuentas?pestana=personal&pagina=2')
    expect(consulta(listados(solicitudes, 'personal').at(-1)!).get('pagina')).toBe('1')
  })

  it('sin gestores ni administradores muestra el estado vacío en la región de estado', async () => {
    montar(RUTA_PERSONAL, { [PERSONAL]: () => respuestaJson(200, paginaAdmin([])) })
    await anunciado('No hay gestores ni administradores.')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('un error de la lista se muestra con «Reintentar», que vuelve a consultar', async () => {
    let intentos = 0
    const { usuario } = montar(RUTA_PERSONAL, {
      [PERSONAL]: () =>
        ++intentos === 1
          ? respuestaProblema(503, 'SERVICIO_NO_DISPONIBLE')
          : respuestaJson(200, paginaAdmin([PERSONAL_GESTOR])),
    })

    expect(await screen.findByRole('alert')).toHaveTextContent('El servicio de cuentas no está disponible en este momento.')
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByText('luis@mail.com')).toBeInTheDocument()
  })

  it('un rol que no es válido en la URL no rompe la pantalla: se consulta sin rol', async () => {
    const { solicitudes } = montar('/admin/cuentas?pestana=personal&rol=Prestador')
    await tabla()
    expect(consulta(listados(solicitudes, 'personal')[0]).get('rol')).toBeNull()
    expect(screen.getByLabelText('Rol')).toHaveValue('')
  })
})

describe('Acciones por fila en «Personal» (DEC-116)', () => {
  it('Gestor → «Pasar a Administrador»; Administrador → «Pasar a Gestor»; «Revocar rol» solo si es revocable', async () => {
    montar(RUTA_PERSONAL)
    await tabla()

    expect(screen.getByRole('button', { name: 'Pasar a Administrador: Luis Gómez' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Pasar a Gestor: Ana Ruiz' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Revocar rol: Luis Gómez' })).not.toHaveAttribute('aria-disabled')
    expect(screen.getByRole('button', { name: 'Revocar rol: Ana Ruiz' })).not.toHaveAttribute('aria-disabled')

    // La cuenta de la semilla se modifica, pero no se revoca: deshabilitado con su motivo (DEC-22, DEC-122).
    expect(screen.getByRole('button', { name: 'Pasar a Administrador: gestor@samaria360.test' })).toBeInTheDocument()
    const revocar = screen.getByRole('button', { name: /^Revocar rol: gestor@samaria360\.test/ })
    expect(revocar).toHaveAttribute('aria-disabled', 'true')
    // jsdom recorta el espacio inicial del texto «(No disponible)» de BotonNoDisponible.
    expect(revocar).toHaveAccessibleName(/^Revocar rol: gestor@samaria360\.test ?\(No disponible\)$/)
    expect(revocar).toHaveAccessibleDescription('Sin datos de turista')
  })

  it('la fila propia dice «Tu cuenta» y no tiene acciones', async () => {
    montar(RUTA_PERSONAL)
    await tabla()

    const propia = fila(USUARIO_ADMIN.correo)
    expect(propia).toHaveTextContent('Tu cuenta')
    expect(within(propia).queryAllByRole('button')).toHaveLength(0)
  })

  it('un «Revocar rol» deshabilitado no abre el diálogo', async () => {
    const { usuario, solicitudes } = montar(RUTA_PERSONAL)
    await usuario.click(await screen.findByRole('button', { name: /^Revocar rol: gestor@samaria360\.test/ }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(cambios(solicitudes)).toHaveLength(0)
  })
})

describe('Cambiar rol desde «Personal» (RF45, DEC-118, DEC-120)', () => {
  it('modificar: el diálogo muestra los roles sin la advertencia de DEC-112, envía {rolNuevo, rolActual} y reemplaza la fila', async () => {
    const { usuario, solicitudes } = montar(RUTA_PERSONAL, {
      [cambioRol(PERSONAL_GESTOR.idUsuario)]: () => respuestaJson(200, { ...PERSONAL_GESTOR, rol: 'Administrador' }),
    })
    await usuario.click(await screen.findByRole('button', { name: 'Pasar a Administrador: Luis Gómez' }))

    expect(dialogo()).toHaveAccessibleName('Cambiar el rol de Luis Gómez')
    expect(dialogo()).toHaveTextContent('Rol actual: Gestor')
    expect(dialogo()).toHaveTextContent('Rol nuevo: Administrador')
    expect(dialogo()).toHaveTextContent('Sus sesiones abiertas se cerrarán y verá el rol nuevo al volver a iniciar sesión.')
    expect(dialogo()).not.toHaveTextContent('Mientras tenga este rol')
    expect(within(dialogo()).getByRole('button', { name: 'Cancelar' })).toHaveFocus()

    await usuario.click(within(dialogo()).getByRole('button', { name: 'Pasar a Administrador' }))

    await anunciado('Luis Gómez ahora es Administrador.')
    expect(cambios(solicitudes).map((s) => s.cuerpo)).toEqual([{ rolNuevo: 'Administrador', rolActual: 'Gestor' }])
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(within(fila('luis@mail.com')).getAllByRole('cell')[2]).toHaveTextContent('Administrador')
    // Se reemplazó en su sitio y el foco va a su acción nueva.
    await waitFor(() => expect(screen.getByRole('button', { name: 'Pasar a Gestor: Luis Gómez' })).toHaveFocus())
    expect(listados(solicitudes, 'personal')).toHaveLength(1)
  })

  it('revocar: avisa que recupera sus datos de turista, envía rolNuevo Turista y la fila sale de «Personal»', async () => {
    let consultas = 0
    const { usuario, solicitudes } = montar(RUTA_PERSONAL, {
      [PERSONAL]: () =>
        respuestaJson(200, paginaAdmin(++consultas === 1 ? [PERSONAL_GESTOR, PERSONAL_ADMIN] : [PERSONAL_GESTOR])),
      [cambioRol(PERSONAL_ADMIN.idUsuario)]: () => respuestaJson(200, { ...PERSONAL_ADMIN, rol: 'Turista' }),
    })
    await usuario.click(await screen.findByRole('button', { name: 'Revocar rol: Ana Ruiz' }))

    expect(dialogo()).toHaveAccessibleName('Revocar el rol de Ana Ruiz')
    expect(dialogo()).toHaveTextContent(
      'La cuenta vuelve a ser Turista: recupera sus reservas, favoritos e historial y pierde los permisos de Administrador.',
    )
    await usuario.click(within(dialogo()).getByRole('button', { name: 'Revocar rol' }))

    await anunciado('Se revocó el rol de Ana Ruiz: vuelve a ser Turista y aparece en la pestaña «Turistas».')
    expect(cambios(solicitudes).map((s) => s.cuerpo)).toEqual([{ rolNuevo: 'Turista', rolActual: 'Administrador' }])
    await waitFor(() => expect(screen.queryByText('ana@mail.com')).not.toBeInTheDocument())
    expect(listados(solicitudes, 'personal')).toHaveLength(2)
    await waitFor(() => expect(titulo()).toHaveFocus())
  })

  it('mientras la petición está pendiente, «Confirmar» queda deshabilitado y Esc no cierra', async () => {
    let responder: (respuesta: Response) => void = () => {}
    const { usuario, solicitudes } = montar(RUTA_PERSONAL, {
      [cambioRol(PERSONAL_GESTOR.idUsuario)]: () => new Promise<Response>((resolver) => (responder = resolver)),
    })
    await usuario.click(await screen.findByRole('button', { name: 'Pasar a Administrador: Luis Gómez' }))
    await usuario.click(within(dialogo()).getByRole('button', { name: 'Pasar a Administrador' }))

    const confirmar = await within(dialogo()).findByRole('button', { name: 'Guardando…' })
    expect(confirmar).toHaveAttribute('aria-disabled', 'true')
    await usuario.click(confirmar)
    await usuario.keyboard('{Escape}')
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(cambios(solicitudes)).toHaveLength(1)

    responder(respuestaJson(200, { ...PERSONAL_GESTOR, rol: 'Administrador' }))
    await anunciado('Luis Gómez ahora es Administrador.')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('«Cancelar» y Esc cierran el diálogo sin enviar nada y devuelven el foco al botón', async () => {
    const { usuario, solicitudes } = montar(RUTA_PERSONAL)
    const boton = await screen.findByRole('button', { name: 'Revocar rol: Luis Gómez' })

    await usuario.click(boton)
    await usuario.click(within(dialogo()).getByRole('button', { name: 'Cancelar' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(boton).toHaveFocus()

    await usuario.click(boton)
    await usuario.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(boton).toHaveFocus()
    expect(cambios(solicitudes)).toHaveLength(0)
  })
})

describe('«Asignar rol» en «Turistas» (RF45, DEC-117)', () => {
  it('solo en las cuentas Activas: en una Desactivada está deshabilitado con su motivo', async () => {
    const { usuario } = montar(RUTA_TURISTAS, {
      [TURISTAS]: () => respuestaJson(200, paginaAdmin([TURISTA_ACTIVO, TURISTA_DESACTIVADO])),
    })
    const activa = await screen.findByRole('button', { name: 'Asignar rol: Marta Lee' })
    expect(activa).not.toHaveAttribute('aria-disabled')

    const desactivada = screen.getByRole('button', { name: /^Asignar rol: Pablo Mora/ })
    expect(desactivada).toHaveAttribute('aria-disabled', 'true')
    expect(desactivada).toHaveAccessibleDescription('La cuenta está desactivada')
    await usuario.click(desactivada)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('el diálogo advierte de DEC-112, envía el rol elegido y, tras el 200, la fila sale de «Turistas»', async () => {
    let consultas = 0
    const { usuario, solicitudes } = montar(RUTA_TURISTAS, {
      [TURISTAS]: () =>
        respuestaJson(200, paginaAdmin(++consultas === 1 ? [TURISTA_ACTIVO, TURISTA_DESACTIVADO] : [TURISTA_DESACTIVADO])),
      [cambioRol(TURISTA_ACTIVO.idUsuario)]: () =>
        respuestaJson(200, {
          idUsuario: TURISTA_ACTIVO.idUsuario,
          nombre: 'Marta Lee',
          correo: TURISTA_ACTIVO.correo,
          rol: 'Administrador',
          fechaRegistro: TURISTA_ACTIVO.fechaRegistro,
          revocable: true,
        }),
    })
    await usuario.click(await screen.findByRole('button', { name: 'Asignar rol: Marta Lee' }))

    expect(dialogo()).toHaveAccessibleName('Asignar rol a Marta Lee')
    expect(dialogo()).toHaveTextContent('Rol actual: Turista')
    expect(dialogo()).toHaveTextContent(
      'Mientras tenga este rol, la cuenta no podrá usar sus reservas, favoritos ni historial de búsquedas. Los datos se conservan y vuelven si se revoca el rol.',
    )
    const grupo = within(dialogo()).getByRole('group', { name: 'Rol nuevo' })
    expect(within(grupo).getByRole('radio', { name: 'Gestor' })).toBeChecked()
    expect(within(grupo).getByRole('radio', { name: 'Gestor' })).toHaveFocus()

    await usuario.click(within(grupo).getByRole('radio', { name: 'Administrador' }))
    await usuario.click(within(dialogo()).getByRole('button', { name: 'Asignar rol' }))

    await anunciado('Marta Lee ahora es Administrador y aparece en la pestaña «Personal».')
    expect(cambios(solicitudes).map((s) => s.cuerpo)).toEqual([{ rolNuevo: 'Administrador', rolActual: 'Turista' }])
    await waitFor(() => expect(screen.queryByText(TURISTA_ACTIVO.correo)).not.toBeInTheDocument())
    expect(listados(solicitudes, 'turistas')).toHaveLength(2)
  })

  it('«Desactivar» sigue siendo la acción que recibe el foco después de desactivar (DEC-52)', async () => {
    const { usuario } = montar(RUTA_TURISTAS, {
      [TURISTAS]: () => respuestaJson(200, paginaAdmin([TURISTA_ACTIVO])),
      [`POST ${BASE}/turistas/${TURISTA_ACTIVO.idUsuario}/desactivacion`]: () =>
        respuestaJson(200, { ...TURISTA_ACTIVO, estado: 'Desactivado' }),
    })
    await usuario.click(await screen.findByRole('button', { name: 'Desactivar: Marta Lee' }))
    await usuario.click(within(dialogo()).getByRole('button', { name: 'Desactivar' }))

    await waitFor(() => expect(screen.getByRole('button', { name: 'Reactivar: Marta Lee' })).toHaveFocus())
    expect(screen.getByRole('button', { name: /^Asignar rol: Marta Lee/ })).toHaveAttribute('aria-disabled', 'true')
  })
})

describe('Errores del cambio de rol (DEC-119, DEC-52)', () => {
  it.each([
    ['ROL_CAMBIADO', 409, { rolActual: 'Administrador' }, 'El rol de la cuenta cambió (ahora es Administrador); se recargó la lista.'],
    ['ROL_CAMBIADO', 409, {}, 'El rol de la cuenta cambió; se recargó la lista.'],
    ['ROL_CAMBIADO', 409, { rolActual: 'Desconocido' }, 'El rol de la cuenta cambió; se recargó la lista.'],
    ['CUENTA_NO_ELEGIBLE', 409, { motivo: 'PRESTADOR' }, 'Las cuentas de prestador no pueden recibir un rol; se recargó la lista.'],
    ['CUENTA_NO_ELEGIBLE', 409, { motivo: 'DESACTIVADA' }, 'La cuenta está desactivada y no puede recibir un rol; se recargó la lista.'],
    [
      'CUENTA_NO_ELEGIBLE',
      409,
      { motivo: 'SIN_ROL_BASE' },
      'La cuenta no tiene datos de turista, así que su rol no se puede revocar; se recargó la lista.',
    ],
    ['CUENTA_NO_ELEGIBLE', 409, { motivo: 'OTRO' }, 'La cuenta no admite el cambio de rol; se recargó la lista.'],
    ['CAMBIO_ROL_PROPIO', 409, {}, 'No puedes cambiar el rol de tu propia cuenta; se recargó la lista.'],
    ['ULTIMO_ADMINISTRADOR', 409, {}, 'Debe quedar al menos un Administrador activo; se recargó la lista.'],
    ['CUENTA_NO_ENCONTRADA', 404, {}, 'La cuenta ya no existe; se recargó la lista.'],
  ])('%s (%i, %o): avisa en la región de estado, cierra el diálogo y recarga', async (codigo, status, extra, mensaje) => {
    const { usuario, solicitudes } = montar(RUTA_PERSONAL, {
      [cambioRol(PERSONAL_ADMIN.idUsuario)]: () => respuestaProblema(status, codigo, extra),
    })
    await usuario.click(await screen.findByRole('button', { name: 'Pasar a Gestor: Ana Ruiz' }))
    await usuario.click(within(dialogo()).getByRole('button', { name: 'Pasar a Gestor' }))

    await anunciado(mensaje)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await waitFor(() => expect(listados(solicitudes, 'personal')).toHaveLength(2))
  })

  it('un 401 SESION_INVALIDADA cierra la sesión y el login dice por qué (DEC-49, DEC-50)', async () => {
    const { usuario } = montar(RUTA_PERSONAL, {
      [cambioRol(PERSONAL_GESTOR.idUsuario)]: () => respuestaProblema(401, 'SESION_INVALIDADA'),
    })
    await usuario.click(await screen.findByRole('button', { name: 'Revocar rol: Luis Gómez' }))
    await usuario.click(within(dialogo()).getByRole('button', { name: 'Revocar rol' }))

    await waitFor(() => expect(ubicacionActual()).toBe('/login'))
    expect(
      await screen.findByText('Tu sesión se cerró porque cambió el estado o los permisos de tu cuenta.'),
    ).toBeInTheDocument()
    expect(sessionStorage.getItem('samaria360.token')).toBeNull()
  })

  it('un error de red se muestra en el diálogo, que sigue abierto, y no recarga la lista', async () => {
    const { usuario, solicitudes } = montar(RUTA_PERSONAL, {
      [cambioRol(PERSONAL_GESTOR.idUsuario)]: () => {
        throw new TypeError('Failed to fetch')
      },
    })
    await usuario.click(await screen.findByRole('button', { name: 'Pasar a Administrador: Luis Gómez' }))
    await usuario.click(within(dialogo()).getByRole('button', { name: 'Pasar a Administrador' }))

    expect(await within(dialogo()).findByRole('alert')).toHaveTextContent(
      'No fue posible conectar con el servidor. Revisa tu conexión y reintenta.',
    )
    expect(within(dialogo()).getByRole('button', { name: 'Pasar a Administrador' })).not.toHaveAttribute('aria-disabled')
    expect(listados(solicitudes, 'personal')).toHaveLength(1)
  })

  it('un 400 DATOS_INVALIDOS muestra el mensaje genérico en el diálogo y no recarga', async () => {
    const { usuario, solicitudes } = montar(RUTA_PERSONAL, {
      [cambioRol(PERSONAL_GESTOR.idUsuario)]: () =>
        respuestaProblema(400, 'DATOS_INVALIDOS', { campos: { rolNuevo: 'NO_PERMITIDO' } }),
    })
    await usuario.click(await screen.findByRole('button', { name: 'Pasar a Administrador: Luis Gómez' }))
    await usuario.click(within(dialogo()).getByRole('button', { name: 'Pasar a Administrador' }))

    expect(await within(dialogo()).findByRole('alert')).toHaveTextContent('Ocurrió un error inesperado. Inténtalo de nuevo.')
    expect(listados(solicitudes, 'personal')).toHaveLength(1)
  })
})

describe('DEC-121: página vacía después de una recarga', () => {
  it('revocar el único elemento de la página 2 lleva a la página 1 con una sola navegación', async () => {
    let revocado = false
    const { usuario, solicitudes } = montar('/admin/cuentas?pestana=personal&pagina=2', {
      [PERSONAL]: (s) => {
        const pagina = Number(consulta(s).get('pagina'))
        const total = revocado ? 10 : 11
        const elementos = pagina === 0 ? [PERSONAL_GESTOR] : revocado ? [] : [PERSONAL_ADMIN]
        return respuestaJson(200, paginaAdmin(elementos, { pagina, totalElementos: total }))
      },
      [cambioRol(PERSONAL_ADMIN.idUsuario)]: () => {
        revocado = true
        return respuestaJson(200, { ...PERSONAL_ADMIN, rol: 'Turista' })
      },
    })
    await usuario.click(await screen.findByRole('button', { name: 'Revocar rol: Ana Ruiz' }))
    await usuario.click(within(dialogo()).getByRole('button', { name: 'Revocar rol' }))

    await waitFor(() => expect(ubicacionActual()).toBe('/admin/cuentas?pestana=personal'))
    expect(await screen.findByText('luis@mail.com')).toBeInTheDocument()
    // El anuncio se conserva después de la navegación.
    await anunciado('Se revocó el rol de Ana Ruiz: vuelve a ser Turista y aparece en la pestaña «Turistas».')
    const paginas = listados(solicitudes, 'personal').map((s) => consulta(s).get('pagina'))
    expect(paginas).toEqual(['1', '1', '0'])
  })

  it('una URL con pagina=9 en «Personal», con 3 páginas, lleva a la página 3', async () => {
    const { solicitudes } = montar('/admin/cuentas?pestana=personal&pagina=9', {
      [PERSONAL]: (s) => {
        const pagina = Number(consulta(s).get('pagina'))
        return respuestaJson(200, paginaAdmin(pagina <= 2 ? [PERSONAL_GESTOR] : [], { pagina, totalElementos: 25 }))
      },
    })

    await waitFor(() => expect(ubicacionActual()).toBe('/admin/cuentas?pestana=personal&pagina=3'))
    expect(await screen.findByText('Página 3 de 3')).toBeInTheDocument()
    expect(listados(solicitudes, 'personal').map((s) => consulta(s).get('pagina'))).toEqual(['8', '2'])
  })

  it('una lista vacía en la página 1 muestra el estado vacío sin navegar', async () => {
    const { solicitudes } = montar(RUTA_PERSONAL, { [PERSONAL]: () => respuestaJson(200, paginaAdmin([])) })

    await anunciado('No hay gestores ni administradores.')
    expect(ubicacionActual()).toBe(RUTA_PERSONAL)
    expect(listados(solicitudes, 'personal')).toHaveLength(1)
  })
})

describe('i18n (RF03, RNF08)', () => {
  it('en inglés: pestaña «Staff», diálogo de asignar con la advertencia y un 409', async () => {
    await i18n.changeLanguage('en')
    const { usuario } = montar(RUTA_TURISTAS, {
      [TURISTAS]: () => respuestaJson(200, paginaAdmin([TURISTA_ACTIVO])),
      [cambioRol(TURISTA_ACTIVO.idUsuario)]: () => respuestaProblema(409, 'ULTIMO_ADMINISTRADOR'),
    })

    expect(await screen.findByRole('tab', { name: 'Staff' })).toBeInTheDocument()
    await usuario.click(await screen.findByRole('button', { name: 'Assign role: Marta Lee' }))
    expect(dialogo()).toHaveAccessibleName('Assign a role to Marta Lee')
    expect(dialogo()).toHaveTextContent('Current role: Tourist')
    expect(dialogo()).toHaveTextContent("While it has this role, the account can't use its bookings, favorites or search history.")
    expect(within(dialogo()).getByRole('radio', { name: 'Manager' })).toBeChecked()
    await usuario.click(within(dialogo()).getByRole('button', { name: 'Assign role' }))

    await anunciado('At least one active Administrator must remain; the list was reloaded.')
  })
})
