import type { MotivoServicio } from '../prestador/rutasCampos'

/**
 * Reglas de las imágenes de un atractivo en el cliente (RF06; DEC-209, DEC-226; las mismas de `PoliticaImagen` en
 * ms-catalog): de 1 a 5, JPEG o PNG reconocidos por su firma (no por la extensión ni por el tipo que declara el
 * navegador) y 5 MB (5 × 1024 × 1024 bytes) como máximo cada una. El servidor sigue siendo la fuente: su 400 marca
 * `imagenes[i]` y su 413 llega como mensaje general.
 */
export const MAXIMO_IMAGENES = 5
export const TAMANO_MAXIMO_IMAGEN = 5 * 1024 * 1024

const FIRMAS: readonly number[][] = [
  [0xff, 0xd8, 0xff],
  [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a],
]

/** Lee los primeros 8 bytes del archivo. */
async function inicio(archivo: Blob): Promise<Uint8Array> {
  const parte = archivo.slice(0, 8)
  if (typeof parte.arrayBuffer === 'function') return new Uint8Array(await parte.arrayBuffer())
  // Respaldo para entornos sin Blob.arrayBuffer (p. ej. jsdom antiguo).
  return new Promise((resolver, rechazar) => {
    const lector = new FileReader()
    lector.onload = () => resolver(new Uint8Array(lector.result as ArrayBuffer))
    lector.onerror = () => rechazar(lector.error)
    lector.readAsArrayBuffer(parte)
  })
}

export async function esJpegOPng(archivo: Blob): Promise<boolean> {
  const bytes = await inicio(archivo)
  return FIRMAS.some((firma) => firma.length <= bytes.length && firma.every((byte, indice) => bytes[indice] === byte))
}

/** Motivo de una imagen; null si sirve. */
export async function motivoImagen(archivo: File): Promise<MotivoServicio | null> {
  if (archivo.size === 0) return 'REQUERIDO'
  if (archivo.size > TAMANO_MAXIMO_IMAGEN) return 'TAMANO_MAXIMO'
  return (await esJpegOPng(archivo)) ? null : 'FORMATO'
}

/** Motivo del grupo por la cantidad; null si está entre 1 y 5. */
export function motivoCantidad(cantidad: number): MotivoServicio | null {
  if (cantidad === 0) return 'REQUERIDO'
  return cantidad > MAXIMO_IMAGENES ? 'CANTIDAD_MAXIMA' : null
}
