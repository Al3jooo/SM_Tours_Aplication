import { describe, expect, it } from 'vitest'
import { TERMINOS } from '../features/contenido/terminos/textoTerminos'
import en from './en.json'
import es from './es.json'

/** Rutas de todas las claves hoja: «pie.terminos», «admin.avisos.tipos.Clima»… */
function claves(objeto: Record<string, unknown>, prefijo = ''): string[] {
  return Object.entries(objeto).flatMap(([clave, valor]) =>
    valor !== null && typeof valor === 'object'
      ? claves(valor as Record<string, unknown>, `${prefijo}${clave}.`)
      : [`${prefijo}${clave}`],
  )
}

/** RNF08: la interfaz completa en español y en inglés. */
describe('Paridad de idiomas (RNF08)', () => {
  it('es.json y en.json tienen exactamente las mismas claves', () => {
    expect(claves(en).sort()).toEqual(claves(es).sort())
  })

  it('ninguna traducción está vacía', () => {
    const vacias = (textos: Record<string, unknown>) =>
      claves(textos).filter((clave) => {
        const valor = clave.split('.').reduce<unknown>((nodo, parte) => (nodo as Record<string, unknown>)[parte], textos)
        return typeof valor === 'string' && valor.trim() === ''
      })
    expect(vacias(es)).toEqual([])
    expect(vacias(en)).toEqual([])
  })

  it('los términos (WF-15) tienen las mismas secciones, en el mismo orden, en los dos idiomas', () => {
    expect(TERMINOS.en.map((seccion) => seccion.id)).toEqual(TERMINOS.es.map((seccion) => seccion.id))
    expect(TERMINOS.es).toHaveLength(9)
    for (const idioma of ['es', 'en'] as const) {
      for (const seccion of TERMINOS[idioma]) expect(seccion.parrafos.length).toBeGreaterThan(0)
    }
  })
})
