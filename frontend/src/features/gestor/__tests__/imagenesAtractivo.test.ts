import { describe, expect, it } from 'vitest'
import { TAMANO_MAXIMO_IMAGEN, esJpegOPng, motivoCantidad, motivoImagen } from '../imagenesAtractivo'

/** RF06 en el cliente (DEC-209, DEC-226): las reglas de `PoliticaImagen` de ms-catalog. */
const JPEG = [0xff, 0xd8, 0xff, 0xe0]
const PNG = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]

function archivo(firma: number[], tamano: number, nombre = 'foto.jpg', tipo = 'image/jpeg'): File {
  const bytes = new Uint8Array(tamano)
  bytes.set(firma.slice(0, tamano))
  return new File([bytes], nombre, { type: tipo })
}

describe('imagenesAtractivo', () => {
  it('reconoce JPEG y PNG por su firma, no por el nombre ni por el tipo declarado', async () => {
    expect(await esJpegOPng(archivo(JPEG, 100, 'foto.png', 'image/png'))).toBe(true)
    expect(await esJpegOPng(archivo(PNG, 100, 'foto.jpg', 'image/jpeg'))).toBe(true)
    expect(await esJpegOPng(new File(['GIF89a...'], 'foto.jpg', { type: 'image/jpeg' }))).toBe(false)
    expect(await esJpegOPng(archivo(PNG, 4))).toBe(false)
  })

  it('5 MB exactos sirven; un byte más es TAMANO_MAXIMO; un archivo vacío es REQUERIDO', async () => {
    expect(TAMANO_MAXIMO_IMAGEN).toBe(5 * 1024 * 1024)
    expect(await motivoImagen(archivo(JPEG, TAMANO_MAXIMO_IMAGEN))).toBeNull()
    expect(await motivoImagen(archivo(JPEG, TAMANO_MAXIMO_IMAGEN + 1))).toBe('TAMANO_MAXIMO')
    expect(await motivoImagen(new File([], 'vacia.jpg', { type: 'image/jpeg' }))).toBe('REQUERIDO')
    expect(await motivoImagen(new File(['texto'], 'falsa.jpg', { type: 'image/jpeg' }))).toBe('FORMATO')
  })

  it('de 1 a 5 imágenes', () => {
    expect(motivoCantidad(0)).toBe('REQUERIDO')
    expect(motivoCantidad(1)).toBeNull()
    expect(motivoCantidad(5)).toBeNull()
    expect(motivoCantidad(6)).toBe('CANTIDAD_MAXIMA')
  })
})
