import { describe, expect, it } from 'vitest'
import { longitudJustificacion, MAXIMO_JUSTIFICACION, validarJustificacion } from '../justificacion'

describe('justificación del rechazo (RF28, DEC-42)', () => {
  it('cuenta sin los espacios de los extremos', () => {
    expect(longitudJustificacion('  RNT vencido \n')).toBe(11)
    expect(longitudJustificacion('     ')).toBe(0)
  })

  it('vacía o en blanco: REQUERIDO', () => {
    expect(validarJustificacion('')).toBe('REQUERIDO')
    expect(validarJustificacion(' \t\n ')).toBe('REQUERIDO')
  })

  it('de 1 a 500 caracteres: válida (los espacios de los extremos no cuentan)', () => {
    expect(validarJustificacion('a')).toBeNull()
    expect(validarJustificacion(`  ${'a'.repeat(MAXIMO_JUSTIFICACION)}  `)).toBeNull()
  })

  it('501 caracteres: LONGITUD_MAXIMA', () => {
    expect(validarJustificacion('a'.repeat(MAXIMO_JUSTIFICACION + 1))).toBe('LONGITUD_MAXIMA')
  })
})
