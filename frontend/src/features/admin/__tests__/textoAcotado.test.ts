import { describe, expect, it } from 'vitest'
import { extracto, longitudTexto, MAXIMO_TITULO, validarTexto } from '../textoAcotado'

describe('Texto acotado del contenido público (DEC-61)', () => {
  it('cuenta en puntos de código y sin los espacios de los extremos: un emoji cuenta uno', () => {
    expect(longitudTexto('  ¿Hay parqueadero? 🚗  ')).toBe(19)
    expect('🚗'.length).toBe(2)
    expect(longitudTexto('🚗')).toBe(1)
  })

  it('vacío o solo con espacios es REQUERIDO; más del máximo es LONGITUD_MAXIMA; justo el máximo vale', () => {
    expect(validarTexto('   ', MAXIMO_TITULO)).toBe('REQUERIDO')
    expect(validarTexto('a'.repeat(151), MAXIMO_TITULO)).toBe('LONGITUD_MAXIMA')
    expect(validarTexto(` ${'a'.repeat(150)} `, MAXIMO_TITULO)).toBeNull()
    // 150 emojis son 300 unidades UTF-16, pero 150 puntos de código: vale.
    expect(validarTexto('🌊'.repeat(150), MAXIMO_TITULO)).toBeNull()
  })

  it('el extracto corta por puntos de código sin partir un emoji', () => {
    expect(extracto('corto', 60)).toBe('corto')
    const largo = `${'a'.repeat(59)}🌊🌊🌊`
    expect(extracto(largo, 60)).toBe(`${'a'.repeat(59)}🌊…`)
    expect([...extracto(largo, 60)].length).toBe(61)
  })
})
