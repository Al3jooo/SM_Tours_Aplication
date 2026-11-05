import { describe, expect, it } from 'vitest'
import i18n from '../../../i18n'
import { borradorEventoVacio, cuerpoEvento, textoErrorEvento, validarBorradorEvento, type BorradorEvento } from '../borradorEvento'

/** Reglas de `EventoTuristico` en el cliente (DEC-236, DEC-237, DEC-248): «no anterior a hoy» queda para el servidor. */
function borrador(cambios: Partial<BorradorEvento> = {}): BorradorEvento {
  return {
    nombre: 'Fiestas del Mar',
    fechaInicio: '2026-11-20',
    fechaFin: '2026-11-22',
    ubicacion: 'Bahía de Santa Marta',
    descripcion: 'Regatas y desfile náutico.',
    ...cambios,
  }
}

describe('Borrador de un evento (WF-25)', () => {
  it('un borrador completo no tiene errores; un evento de un día también es válido', () => {
    expect(validarBorradorEvento(borrador())).toEqual({})
    expect(validarBorradorEvento(borrador({ fechaFin: '2026-11-20' }))).toEqual({})
  })

  it('vacío: los cinco campos son obligatorios (DEC-235)', () => {
    expect(validarBorradorEvento(borradorEventoVacio())).toEqual({
      nombre: 'REQUERIDO',
      ubicacion: 'REQUERIDO',
      descripcion: 'REQUERIDO',
      fechaInicio: 'REQUERIDO',
      fechaFin: 'REQUERIDO',
    })
    expect(validarBorradorEvento(borrador({ nombre: '   ' }))).toEqual({ nombre: 'REQUERIDO' })
  })

  it('cuenta en puntos de código y sin los espacios de los extremos: 120, 200 y 1000', () => {
    expect(validarBorradorEvento(borrador({ nombre: ` ${'🎉'.repeat(120)} ` }))).toEqual({})
    expect(validarBorradorEvento(borrador({ nombre: 'a'.repeat(121) }))).toEqual({ nombre: 'LONGITUD_MAXIMA' })
    expect(validarBorradorEvento(borrador({ ubicacion: 'a'.repeat(200) }))).toEqual({})
    expect(validarBorradorEvento(borrador({ ubicacion: 'a'.repeat(201) }))).toEqual({ ubicacion: 'LONGITUD_MAXIMA' })
    expect(validarBorradorEvento(borrador({ descripcion: 'a'.repeat(1000) }))).toEqual({})
    expect(validarBorradorEvento(borrador({ descripcion: 'a'.repeat(1001) }))).toEqual({ descripcion: 'LONGITUD_MAXIMA' })
  })

  it('una fecha que no es un día «yyyy-MM-dd» que existe es FORMATO', () => {
    expect(validarBorradorEvento(borrador({ fechaInicio: '2026-02-30' }))).toEqual({ fechaInicio: 'FORMATO' })
    expect(validarBorradorEvento(borrador({ fechaFin: '20/11/2026' }))).toEqual({ fechaFin: 'FORMATO' })
  })

  it('un fin anterior al inicio es NO_PERMITIDO en el fin (DEC-237)', () => {
    expect(validarBorradorEvento(borrador({ fechaInicio: '2026-11-22', fechaFin: '2026-11-20' }))).toEqual({
      fechaFin: 'NO_PERMITIDO',
    })
  })

  it('no calcula «hoy»: un evento que ya terminó pasa al servidor (DEC-242, DEC-248)', () => {
    expect(validarBorradorEvento(borrador({ fechaInicio: '2000-01-01', fechaFin: '2000-01-02' }))).toEqual({})
  })

  it('el cuerpo va sin los espacios de los extremos', () => {
    expect(cuerpoEvento(borrador({ nombre: '  Fiestas del Mar ', descripcion: '\nRegatas\n' }))).toEqual({
      nombre: 'Fiestas del Mar',
      fechaInicio: '2026-11-20',
      fechaFin: '2026-11-22',
      ubicacion: 'Bahía de Santa Marta',
      descripcion: 'Regatas',
    })
  })

  it('NO_PERMITIDO en el fin se explica según el orden del borrador; el resto, con su máximo', () => {
    const t = i18n.getFixedT('es')
    expect(textoErrorEvento(t, 'fechaFin', 'NO_PERMITIDO', borrador({ fechaInicio: '2026-11-22', fechaFin: '2026-11-20' }))).toBe(
      'La fecha de fin no puede ser anterior a la de inicio.',
    )
    expect(textoErrorEvento(t, 'fechaFin', 'NO_PERMITIDO', borrador({ fechaInicio: '2026-11-01', fechaFin: '2026-11-01' }))).toBe(
      'La fecha de fin ya pasó (hoy en Santa Marta). Un evento terminado no aparecería en la agenda.',
    )
    expect(textoErrorEvento(t, 'descripcion', 'LONGITUD_MAXIMA', borrador())).toBe('Usa como máximo 1000 caracteres.')
    expect(textoErrorEvento(t, 'nombre', undefined, borrador())).toBeUndefined()
  })
})
