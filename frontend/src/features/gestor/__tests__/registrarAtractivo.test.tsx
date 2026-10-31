import { screen, waitFor, within } from '@testing-library/react'
import type { UserEvent } from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
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

/** WF-09 «Registrar atractivo» (CU «Registrar atractivo» con «Cargar imágenes»; DEC-216, DEC-226, DEC-230). */
const ME = 'GET /api/v1/auth/me'
const REGISTRO = 'POST /api/v1/catalogo/gestor/atractivos'
const LISTA = 'GET /api/v1/catalogo/gestor/atractivos'
const LENTO = { timeout: 15_000 }
const JPEG = [0xff, 0xd8, 0xff, 0xe0]
const PNG = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]

function archivo(nombre: string, firma: number[], tamano = 64, tipo = 'image/jpeg'): File {
  const bytes = new Uint8Array(tamano)
  bytes.set(firma.slice(0, tamano))
  return new File([bytes], nombre, { type: tipo })
}

function conGestor(rutas: Parameters<typeof simularFetch>[0] = {}) {
  sessionStorage.setItem('samaria360.token', 'token-guardado')
  return simularFetch({
    [ME]: () => respuestaJson(200, USUARIO_GESTOR),
    [LISTA]: () => respuestaJson(200, paginaAtractivos([elementoAtractivo()])),
    ...rutas,
  })
}

/** jsdom no tiene URL.createObjectURL: vistas previas con una URL fija por archivo. */
beforeEach(() => {
  let n = 0
  vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: vi.fn(() => `blob:vista-${(n += 1)}`), revokeObjectURL: vi.fn() }))
})

function leerTexto(parte: Blob): Promise<string> {
  return new Promise((resolver) => {
    const lector = new FileReader()
    lector.onload = () => resolver(String(lector.result))
    lector.readAsText(parte)
  })
}

async function llenar(usuario: UserEvent) {
  await usuario.type(await screen.findByLabelText('Nombre del atractivo', {}, LENTO), 'Sendero Cascada El Salto')
  await usuario.selectOptions(screen.getByLabelText('Categoría'), 'Naturaleza')
  await usuario.type(screen.getByLabelText('Descripción'), 'Sendero entre bosque húmedo.')
  await usuario.type(screen.getByLabelText('Latitud'), '11.15')
  await usuario.type(screen.getByLabelText('Longitud'), '-74.11')
  await usuario.click(screen.getByRole('checkbox', { name: 'Movilidad reducida' }))
  await usuario.type(screen.getByLabelText('Límite máximo de capacidad de carga'), '80')
  await usuario.selectOptions(screen.getByLabelText('Estado operativo'), 'Cerrado_temporalmente')
  await usuario.type(screen.getByLabelText(/^Horario de atención/), 'Martes a domingo')
  await usuario.type(screen.getByRole('textbox', { name: /^Normas y regulaciones ambientales/ }), 'Sin fogatas.')
}

const registros = (solicitudes: SolicitudSimulada[]) => solicitudes.filter((s) => `${s.metodo} ${s.url}` === REGISTRO)

describe('WF-09 «Registrar atractivo»', () => {
  it('envía un multipart con la parte datos en JSON y las imágenes, y vuelve al panel con el anuncio', async () => {
    const { solicitudes } = conGestor({ [REGISTRO]: () => respuestaJson(201, atractivoGestion({ version: 0 })) })
    const { usuario } = renderApp('/gestor/atractivos/nuevo')

    await llenar(usuario)
    await usuario.upload(screen.getByLabelText('Agregar fotos'), [archivo('cascada.jpg', JPEG), archivo('sendero.png', PNG, 64, 'image/png')])
    expect(await screen.findByText('2 de 5 fotos.')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Vista previa de la foto 2 (sendero.png)' })).toHaveAttribute('src', 'blob:vista-2')
    await usuario.click(screen.getByRole('button', { name: 'Registrar atractivo' }))

    await waitFor(() => expect(ubicacionActual()).toBe('/gestor/atractivos'))
    expect(await screen.findByText('Atractivo registrado: ya es visible en la búsqueda.', {}, LENTO)).toBeInTheDocument()
    const [registro] = registros(solicitudes)
    expect(registro.cabeceras['Content-Type']).toBeUndefined()
    expect(registro.cabeceras.Authorization).toBe('Bearer token-guardado')
    const datos = registro.formulario!.get('datos') as Blob
    expect(datos.type).toBe('application/json')
    expect(JSON.parse(await leerTexto(datos))).toEqual({
      nombre: 'Sendero Cascada El Salto',
      categoria: 'Naturaleza',
      descripcion: 'Sendero entre bosque húmedo.',
      coordenadas: { longitud: -74.11, latitud: 11.15 },
      limiteCapacidadCarga: 80,
      estadoOperativo: 'Cerrado_temporalmente',
      normasAmbientales: 'Sin fogatas.',
      horarioAtencion: 'Martes a domingo',
      condicionesAccesibilidad: ['MovilidadReducida'],
    })
    expect((registro.formulario!.getAll('imagenes') as File[]).map((imagen) => imagen.name)).toEqual(['cascada.jpg', 'sendero.png'])
  })

  it('sin datos ni fotos no envía nada y marca cada obligatorio, también las fotos', async () => {
    const { solicitudes } = conGestor()
    const { usuario } = renderApp('/gestor/atractivos/nuevo')

    await usuario.click(await screen.findByRole('button', { name: 'Registrar atractivo' }, LENTO))

    expect(screen.getByLabelText('Nombre del atractivo')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Límite máximo de capacidad de carga')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Agrega al menos una foto.')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByLabelText('Nombre del atractivo')).toHaveFocus())
    expect(registros(solicitudes)).toHaveLength(0)
  })

  it('una foto que no es JPEG ni PNG o de más de 5 MB se marca junto a su miniatura y no se envía', async () => {
    const { solicitudes } = conGestor()
    const { usuario } = renderApp('/gestor/atractivos/nuevo')

    await llenar(usuario)
    await usuario.upload(screen.getByLabelText('Agregar fotos'), [
      archivo('ok.jpg', JPEG),
      new File(['no soy una imagen'], 'falsa.jpg', { type: 'image/jpeg' }),
      archivo('grande.jpg', JPEG, 5 * 1024 * 1024 + 1),
    ])

    const falsa = (await screen.findByText('No es una imagen JPEG ni PNG.', {}, LENTO)).closest('li')!
    expect(within(falsa).getByText('falsa.jpg')).toBeInTheDocument()
    const grande = screen.getByText('La foto supera los 5 MB.').closest('li')!
    expect(within(grande).getByText('grande.jpg')).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Registrar atractivo' }))
    expect(registros(solicitudes)).toHaveLength(0)

    // Quitar las dos inválidas deja el formulario listo; el foco pasa al «Quitar» anterior.
    await usuario.click(screen.getByRole('button', { name: 'Quitar la foto 3' }))
    await usuario.click(screen.getByRole('button', { name: 'Quitar la foto 2' }))
    expect(screen.queryByText('No es una imagen JPEG ni PNG.')).not.toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Quitar la foto 1' })).toHaveFocus())
  })

  it('no deja pasar de 5 fotos y avisa cuántas no se agregaron', async () => {
    conGestor()
    const { usuario } = renderApp('/gestor/atractivos/nuevo')

    const seis = Array.from({ length: 6 }, (_, indice) => archivo(`foto-${indice}.jpg`, JPEG))
    await usuario.upload(await screen.findByLabelText('Agregar fotos', {}, LENTO), seis)

    expect(await screen.findByText(/5 de 5 fotos\. 1 foto no se agregó: el máximo es 5\./)).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /^Quitar la foto/ })).toHaveLength(5)
    expect(screen.getByLabelText('Agregar fotos')).toBeDisabled()
  })

  it('un 400 con imagenes[i] se marca en esa miniatura; un 413 es un mensaje general y no borra lo escrito', async () => {
    let intentos = 0
    conGestor({
      [REGISTRO]: () =>
        ++intentos === 1
          ? respuestaProblema(400, 'DATOS_INVALIDOS', { campos: { 'imagenes[1]': 'FORMATO' } })
          : respuestaProblema(413, 'CARGA_DEMASIADO_GRANDE'),
    })
    const { usuario } = renderApp('/gestor/atractivos/nuevo')

    await llenar(usuario)
    await usuario.upload(screen.getByLabelText('Agregar fotos'), [archivo('a.jpg', JPEG), archivo('b.jpg', JPEG)])
    await usuario.click(screen.getByRole('button', { name: 'Registrar atractivo' }))

    expect(await screen.findByText('No es una imagen JPEG ni PNG.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Quitar la foto 2' })).toHaveAttribute('aria-describedby')

    await usuario.click(screen.getByRole('button', { name: 'Registrar atractivo' }))
    expect(
      await screen.findByText('Las fotos superan el tamaño que admite el servidor. Quita alguna o usa archivos más livianos.'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Nombre del atractivo')).toHaveValue('Sendero Cascada El Salto')
    expect(screen.getAllByRole('button', { name: /^Quitar la foto/ })).toHaveLength(2)
  })

  it('C7: los contadores cuentan puntos de código: 1000 en la descripción y 500 en el horario', async () => {
    conGestor()
    const { usuario } = renderApp('/gestor/atractivos/nuevo')

    await usuario.type(await screen.findByLabelText('Descripción', {}, LENTO), '🌴🌴')
    await usuario.type(screen.getByLabelText(/^Horario de atención/), '🌴')

    expect(screen.getByText('2/1000')).toBeInTheDocument()
    expect(screen.getByText('1/500')).toBeInTheDocument()
    expect(screen.getByText('0/1000')).toBeInTheDocument()
    expect(screen.getByText('0/120')).toBeInTheDocument()
  })

  it('la casilla de veracidad se ve deshabilitada y sin marca de obligatoria (DEC-219, DEC-228)', async () => {
    conGestor()
    renderApp('/gestor/atractivos/nuevo')

    const casilla = await screen.findByRole('group', { name: 'Confirmo que la información es veraz y está actualizada.' }, LENTO)
    expect(casilla).toHaveAttribute('aria-disabled', 'true')
    expect(within(casilla).queryByRole('checkbox')).not.toBeInTheDocument()
    expect(casilla.textContent).not.toContain('*')
  })
})
