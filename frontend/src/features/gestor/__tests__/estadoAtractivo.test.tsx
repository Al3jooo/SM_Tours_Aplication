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
import { renderApp } from '../../../test/renderApp'

/** CU «Cambiar estado operativo» (RF33; DEC-210, DEC-225) desde el panel y desde la edición. */
const ME = 'GET /api/v1/auth/me'
const LISTA = 'GET /api/v1/catalogo/gestor/atractivos'
const RUTA = `/api/v1/catalogo/gestor/atractivos/${ID_ATRACTIVO}`
const ESTADO = `POST ${RUTA}/estado`
const LENTO = { timeout: 15_000 }

function conGestor(rutas: Parameters<typeof simularFetch>[0]) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  return simularFetch({ [ME]: () => respuestaJson(200, USUARIO_GESTOR), ...rutas })
}

const cambios = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => `${s.metodo} ${s.url}` === ESTADO)

describe('Cambiar estado operativo', () => {
  it('desde el panel: el estado actual no se puede elegir; al guardar, la fila y el anuncio muestran el nuevo', async () => {
    let lecturas = 0
    const { solicitudes } = conGestor({
      [LISTA]: () =>
        respuestaJson(
          200,
          paginaAtractivos([++lecturas === 1 ? elementoAtractivo() : elementoAtractivo({ estadoOperativo: 'En_mantenimiento', version: 3 })]),
        ),
      [ESTADO]: () => respuestaJson(200, atractivoGestion({ estadoOperativo: 'En_mantenimiento', version: 3 })),
    })
    const { usuario } = renderApp('/gestor/atractivos')

    await usuario.click(await screen.findByRole('button', { name: 'Cambiar el estado de Cascada El Salto' }, LENTO))
    const dialogo = within(screen.getByRole('dialog', { name: 'Cambiar el estado de Cascada El Salto' }))
    expect(dialogo.getByText('Estado actual: Abierto.')).toBeInTheDocument()
    expect(dialogo.getByRole('radio', { name: /^Abierto/ })).toBeDisabled()
    await usuario.click(dialogo.getByRole('radio', { name: 'En mantenimiento' }))
    await usuario.click(dialogo.getByRole('button', { name: 'Cambiar estado' }))

    expect(await screen.findByText('Cascada El Salto ahora está «En mantenimiento».')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(cambios(solicitudes)[0].cuerpo).toEqual({ estadoNuevo: 'En_mantenimiento', version: 2 })
    await waitFor(() => expect(screen.getByRole('table')).toHaveTextContent('En mantenimiento'))
  })

  it('con 409 relee: si otro gestor eligió otro estado, conserva lo elegido y reenvía con la versión nueva', async () => {
    let envios = 0
    const { solicitudes } = conGestor({
      [LISTA]: () => respuestaJson(200, paginaAtractivos([elementoAtractivo()])),
      [`GET ${RUTA}`]: () => respuestaJson(200, atractivoGestion({ estadoOperativo: 'Cerrado_temporalmente', version: 3 })),
      [ESTADO]: () =>
        ++envios === 1
          ? respuestaProblema(409, 'ATRACTIVO_MODIFICADO', { versionActual: 3 })
          : respuestaJson(200, atractivoGestion({ estadoOperativo: 'En_mantenimiento', version: 4 })),
    })
    const { usuario } = renderApp('/gestor/atractivos')

    await usuario.click(await screen.findByRole('button', { name: 'Cambiar el estado de Cascada El Salto' }, LENTO))
    const dialogo = within(screen.getByRole('dialog'))
    await usuario.click(dialogo.getByRole('radio', { name: 'En mantenimiento' }))
    await usuario.click(dialogo.getByRole('button', { name: 'Cambiar estado' }))

    expect(
      await dialogo.findByText(
        'Otro gestor cambió el atractivo; ahora está «Cerrado temporalmente». Confirma de nuevo si quieres el estado elegido.',
      ),
    ).toBeInTheDocument()
    expect(dialogo.getByRole('radio', { name: 'En mantenimiento' })).toBeChecked()
    await usuario.click(dialogo.getByRole('button', { name: 'Cambiar estado' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(cambios(solicitudes).map((s) => s.cuerpo)).toEqual([
      { estadoNuevo: 'En_mantenimiento', version: 2 },
      { estadoNuevo: 'En_mantenimiento', version: 3 },
    ])
  })

  it('con 409 y el mismo estado elegido por otro gestor, avisa y solo queda cerrar', async () => {
    const { solicitudes } = conGestor({
      [LISTA]: () => respuestaJson(200, paginaAtractivos([elementoAtractivo()])),
      [`GET ${RUTA}`]: () => respuestaJson(200, atractivoGestion({ estadoOperativo: 'En_mantenimiento', version: 3 })),
      [ESTADO]: () => respuestaProblema(409, 'ATRACTIVO_MODIFICADO', { versionActual: 3 }),
    })
    const { usuario } = renderApp('/gestor/atractivos')

    await usuario.click(await screen.findByRole('button', { name: 'Cambiar el estado de Cascada El Salto' }, LENTO))
    const dialogo = within(screen.getByRole('dialog'))
    await usuario.click(dialogo.getByRole('radio', { name: 'En mantenimiento' }))
    await usuario.click(dialogo.getByRole('button', { name: 'Cambiar estado' }))

    expect(await dialogo.findByText('Otro gestor ya lo dejó «En mantenimiento». No hay nada que cambiar.')).toBeInTheDocument()
    expect(dialogo.getByRole('button', { name: 'Cambiar estado' })).toBeDisabled()
    await usuario.click(dialogo.getByRole('button', { name: 'Cerrar' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(cambios(solicitudes)).toHaveLength(1)
  })

  it('desde la edición: guardar el estado no toca lo escrito y la edición sale con la versión nueva', async () => {
    const { solicitudes } = conGestor({
      [LISTA]: () => respuestaJson(200, paginaAtractivos([elementoAtractivo()])),
      [`GET ${RUTA}`]: () => respuestaJson(200, atractivoGestion()),
      [ESTADO]: () => respuestaJson(200, atractivoGestion({ estadoOperativo: 'Cerrado_temporalmente', version: 3 })),
      [`PUT ${RUTA}`]: () => respuestaJson(200, atractivoGestion({ version: 4 })),
    })
    const { usuario } = renderApp(`/gestor/atractivos/${ID_ATRACTIVO}`)

    const nombre = await screen.findByLabelText('Nombre del atractivo', {}, LENTO)
    await usuario.type(nombre, ' de Minca')
    await usuario.click(screen.getByRole('button', { name: 'Cambiar estado' }))
    const dialogo = within(screen.getByRole('dialog'))
    await usuario.click(dialogo.getByRole('radio', { name: 'Cerrado temporalmente' }))
    await usuario.click(dialogo.getByRole('button', { name: 'Cambiar estado' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(screen.getByLabelText('Nombre del atractivo')).toHaveValue('Cascada El Salto de Minca')
    expect(screen.getByText('Cerrado temporalmente')).toHaveClass('insignia-estado--Cerrado_temporalmente')
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    await waitFor(() => expect(solicitudes.some((s) => s.metodo === 'PUT')).toBe(true))
    expect(solicitudes.find((s) => s.metodo === 'PUT')!.cuerpo).toMatchObject({ nombre: 'Cascada El Salto de Minca', version: 3 })
  })
})
