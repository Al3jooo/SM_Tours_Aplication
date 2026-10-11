import { describe, expect, it } from 'vitest'
import { iniciales } from './iniciales'

describe('iniciales', () => {
  it('usa la primera letra de la primera y de la última palabra', () => {
    expect(iniciales('Ana Pérez')).toBe('AP')
    expect(iniciales('María José Gómez Ruiz')).toBe('MR')
  })

  it('con una sola palabra usa su primera letra', () => {
    expect(iniciales('gestor')).toBe('G')
  })

  it('tolera espacios sobrantes, tildes y nombres vacíos', () => {
    expect(iniciales('  ángela   ruiz ')).toBe('ÁR')
    expect(iniciales('   ')).toBe('?')
  })
})
