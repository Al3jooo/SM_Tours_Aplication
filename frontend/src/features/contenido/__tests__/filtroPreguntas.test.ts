import { describe, expect, it } from 'vitest'
import type { PreguntaFrecuente } from '../contenidoApi'
import { filtrarPreguntas, indexarPreguntas, normalizarTexto } from '../filtroPreguntas'

const pregunta = (id: string, titulo: string, cuerpo: string): PreguntaFrecuente => ({
  id,
  titulo,
  cuerpo,
  fechaActualizacion: '2026-10-22T13:00:00Z',
})

const PREGUNTAS = [
  pregunta('1', '¿Cómo encuentro lugares accesibles?', 'Usa el filtro de accesibilidad.'),
  pregunta('2', '¿Hasta cuándo puedo cancelar?', 'Hasta 24 horas antes del inicio.'),
  pregunta('3', '¿Qué incluye el Tour de Café?', 'Un recorrido por fincas de la Sierra Nevada.'),
]

const titulos = (termino: string) => filtrarPreguntas(indexarPreguntas(PREGUNTAS), termino).map((p) => p.id)

describe('normalizarTexto (RF47, DEC-71)', () => {
  it('quita tildes y diéresis, pasa a minúsculas y recorta los extremos', () => {
    expect(normalizarTexto('  Café PINGÜINO Ñandú ')).toBe('cafe pinguino nandu')
  })
})

describe('filtrarPreguntas (RF47, DEC-62)', () => {
  it('«cafe» encuentra «Café»', () => {
    expect(titulos('cafe')).toEqual(['3'])
  })

  it('no distingue mayúsculas', () => {
    expect(titulos('TOUR DE CAFÉ')).toEqual(['3'])
    expect(titulos('cómo ENCUENTRO')).toEqual(['1'])
  })

  it('coincide en el cuerpo y de forma parcial', () => {
    expect(titulos('sierra')).toEqual(['3'])
    expect(titulos('hora')).toEqual(['2'])
  })

  it('un término vacío o solo con espacios devuelve todas, en el orden del servidor', () => {
    expect(titulos('')).toEqual(['1', '2', '3'])
    expect(titulos('   ')).toEqual(['1', '2', '3'])
  })

  it('sin coincidencias devuelve una lista vacía', () => {
    expect(titulos('parqueadero')).toEqual([])
  })

  it('una coincidencia no cruza del título al cuerpo', () => {
    expect(titulos('accesibles? usa')).toEqual([])
  })
})
