import { screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  ID_ATRACTIVO,
  USUARIO_GESTOR,
  atractivoGestion,
  elementoAtractivo,
  paginaAtractivos,
  respuestaJson,
  respuestaProblema,
  simularFetch,
  type SolicitudSimulada,
} from '../../../test/fetchSimulado'
import { renderApp, ubicacionActual } from '../../../test/renderApp'

/** WF-09 en modo edición (CU «Actualizar atractivo»; DEC-207, DEC-226, DEC-231). */
const ME = 'GET /api/v1/auth/me'
const RUTA = `/api/v1/catalogo/gestor/atractivos/${ID_ATRACTIVO}`
const LISTA = 'GET /api/v1/catalogo/gestor/atractivos'
const LENTO = { timeout: 15_000 }

function conGestor(rutas: Parameters<typeof simularFetch>[0]) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  return simularFetch({
    [ME]: () => respuestaJson(200, USUARIO_GESTOR),
    [LISTA]: () => respuestaJson(200, paginaAtractivos([elementoAtractivo()])),
    ...rutas,
  })
}

const ediciones = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => s.metodo === 'PUT' && s.url === RUTA)

describe('WF-09 en edición', () => {
  it('carga los datos, muestra las fotos en solo lectura y guarda con la versión leída, sin el estado', async () => {
    const { solicitudes } = conGestor({
      [`GET ${RUTA}`]: () => respuestaJson(200, atractivoGestion()),
      [`PUT ${RUTA}`]: () => respuestaJson(200, atractivoGestion({ version: 3 })),
    })
    const { usuario } = renderApp(`/gestor/atractivos/${ID_ATRACTIVO}`)

    const nombre = await screen.findByLabelText('Nombre del atractivo', {}, LENTO)
    expect(nombre).toHaveValue('Cascada El Salto')
    expect(screen.getByLabelText('Límite máximo de capacidad de carga')).toHaveValue('80')
    expect(screen.queryByLabelText('Estado operativo')).not.toBeInTheDocument()
    // DEC-226: las fotos se ven, pero no se pueden agregar ni quitar.
    const fotos = screen.getByRole('group', { name: 'Fotos del atractivo' })
    expect(within(fotos).getByRole('img', { name: 'Foto 1 de Cascada El Salto' })).toBeInTheDocument()
    expect(within(fotos).queryByLabelText('Agregar fotos')).not.toBeInTheDocument()
    expect(within(fotos).getByText('Agregar o quitar fotos').closest('[aria-disabled="true"]')).not.toBeNull()

    await usuario.clear(screen.getByLabelText('Límite máximo de capacidad de carga'))
    await usuario.type(screen.getByLabelText('Límite máximo de capacidad de carga'), '120')
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    await waitFor(() => expect(ubicacionActual()).toBe('/gestor/atractivos'))
    expect(await screen.findByText('Cambios guardados.', {}, LENTO)).toBeInTheDocument()
    const [edicion] = ediciones(solicitudes)
    expect(edicion.cuerpo).toMatchObject({ nombre: 'Cascada El Salto', limiteCapacidadCarga: 120, version: 2 })
    expect(edicion.cuerpo).not.toHaveProperty('estadoOperativo')
  })

  it('con 409 ATRACTIVO_MODIFICADO relee, conserva lo escrito, muestra el valor actual y reenvía con la versión nueva', async () => {
    let lecturas = 0
    let envios = 0
    const { solicitudes } = conGestor({
      [`GET ${RUTA}`]: () =>
        respuestaJson(200, ++lecturas === 1 ? atractivoGestion() : atractivoGestion({ version: 3, normasAmbientales: 'Sin mascotas.' })),
      [`PUT ${RUTA}`]: () =>
        ++envios === 1
          ? respuestaProblema(409, 'ATRACTIVO_MODIFICADO', { versionActual: 3 })
          : respuestaJson(200, atractivoGestion({ version: 4 })),
    })
    const { usuario } = renderApp(`/gestor/atractivos/${ID_ATRACTIVO}`)

    const nombre = await screen.findByLabelText('Nombre del atractivo', {}, LENTO)
    await usuario.clear(nombre)
    await usuario.type(nombre, 'Cascada El Salto de Minca')
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    expect(
      await screen.findByText('Otro gestor cambió este atractivo. Revisa los valores actuales y vuelve a guardar.'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Nombre del atractivo')).toHaveValue('Cascada El Salto de Minca')
    expect(within(screen.getByRole('list', { name: 'Lo que otro gestor cambió' })).getByText(/Valor actual: Sin mascotas\./)).toBeInTheDocument()
    expect(ubicacionActual()).toBe(`/gestor/atractivos/${ID_ATRACTIVO}`)

    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    await waitFor(() => expect(ubicacionActual()).toBe('/gestor/atractivos'))
    expect(ediciones(solicitudes).map((s) => (s.cuerpo as { version: number }).version)).toEqual([2, 3])
  })

  it('un 400 marca el campo y un atractivo inexistente no muestra el formulario', async () => {
    conGestor({
      [`GET ${RUTA}`]: () => respuestaJson(200, atractivoGestion()),
      [`PUT ${RUTA}`]: () => respuestaProblema(400, 'DATOS_INVALIDOS', { campos: { descripcion: 'LONGITUD_MAXIMA' } }),
    })
    const { usuario } = renderApp(`/gestor/atractivos/${ID_ATRACTIVO}`)

    await usuario.click(await screen.findByRole('button', { name: 'Guardar cambios' }, LENTO))
    expect(await screen.findByText('Usa como máximo 1000 caracteres.')).toBeInTheDocument()
    expect(screen.getByLabelText('Descripción')).toHaveAttribute('aria-invalid', 'true')
  })

  it('un 404 al cargar dice que no se encontró, sin formulario', async () => {
    conGestor({ [`GET ${RUTA}`]: () => respuestaProblema(404, 'ATRACTIVO_NO_ENCONTRADO') })
    renderApp(`/gestor/atractivos/${ID_ATRACTIVO}`)

    expect(await screen.findByText('No se encontró el atractivo.', {}, LENTO)).toBeInTheDocument()
    expect(screen.queryByLabelText('Nombre del atractivo')).not.toBeInTheDocument()
  })
})
