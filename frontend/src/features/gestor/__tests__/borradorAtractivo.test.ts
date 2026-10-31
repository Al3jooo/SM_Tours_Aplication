import { describe, expect, it } from 'vitest'
import { atractivoGestion } from '../../../test/fetchSimulado'
import type { AtractivoGestion } from '../atractivosGestorApi'
import {
  borradorDe,
  borradorVacio,
  construirDatos,
  rutasConControl,
  validarBorrador,
  type BorradorAtractivo,
} from '../borradorAtractivo'

/** Reglas de WF-09 en el cliente: las de `RegistroAtractivo` (ms-catalog; DEC-207, DEC-212, DEC-227). */
const valido = (): BorradorAtractivo => ({
  ...borradorVacio(),
  nombre: 'Cascada El Salto',
  categoria: 'Naturaleza',
  descripcion: 'Sendero',
  latitud: '11.15',
  longitud: '-74.11',
  limiteCapacidadCarga: '80',
})

describe('borradorAtractivo', () => {
  it('un borrador completo no tiene errores; el alta exige el estado y la edición no', () => {
    expect(validarBorrador(valido(), true)).toEqual({})
    expect(validarBorrador({ ...valido(), estadoOperativo: '' }, true)).toEqual({ estadoOperativo: 'REQUERIDO' })
    expect(validarBorrador({ ...valido(), estadoOperativo: '' }, false)).toEqual({})
  })

  it('obligatorios, formatos y rangos con los nombres JSON del contrato', () => {
    expect(validarBorrador({ ...borradorVacio(), latitud: 'x', longitud: '200' }, true)).toEqual({
      nombre: 'REQUERIDO',
      categoria: 'REQUERIDO',
      descripcion: 'REQUERIDO',
      'coordenadas.latitud': 'FORMATO',
      'coordenadas.longitud': 'VALOR_MAXIMO',
      limiteCapacidadCarga: 'REQUERIDO',
    })
    expect(validarBorrador({ ...valido(), limiteCapacidadCarga: '0' }, true)).toEqual({ limiteCapacidadCarga: 'VALOR_MINIMO' })
    expect(validarBorrador({ ...valido(), limiteCapacidadCarga: '100001' }, true)).toEqual({
      limiteCapacidadCarga: 'VALOR_MAXIMO',
    })
    expect(validarBorrador({ ...valido(), limiteCapacidadCarga: '1,5' }, true)).toEqual({ limiteCapacidadCarga: 'FORMATO' })
  })

  it('C7: las longitudes se cuentan en puntos de código: 120, 1000 en la descripción y las normas, 500 en el horario', () => {
    const emoji = '🌴'
    const justo = {
      ...valido(),
      nombre: emoji.repeat(120),
      descripcion: emoji.repeat(1000),
      normasAmbientales: emoji.repeat(1000),
      horarioAtencion: emoji.repeat(500),
    }
    expect(validarBorrador(justo, true)).toEqual({})
    expect(
      validarBorrador(
        {
          ...justo,
          nombre: emoji.repeat(121),
          descripcion: emoji.repeat(1001),
          normasAmbientales: emoji.repeat(1001),
          horarioAtencion: emoji.repeat(501),
        },
        true,
      ),
    ).toEqual({
      nombre: 'LONGITUD_MAXIMA',
      descripcion: 'LONGITUD_MAXIMA',
      normasAmbientales: 'LONGITUD_MAXIMA',
      horarioAtencion: 'LONGITUD_MAXIMA',
    })
  })

  it('construirDatos: coordenadas en número y el estado solo en el alta', () => {
    const datos = construirDatos({ ...valido(), condicionesAccesibilidad: ['Visual'] }, true)
    expect(datos).toEqual({
      nombre: 'Cascada El Salto',
      categoria: 'Naturaleza',
      descripcion: 'Sendero',
      coordenadas: { longitud: -74.11, latitud: 11.15 },
      limiteCapacidadCarga: 80,
      estadoOperativo: 'Abierto',
      normasAmbientales: '',
      horarioAtencion: '',
      condicionesAccesibilidad: ['Visual'],
    })
    expect(construirDatos(valido(), false)).not.toHaveProperty('estadoOperativo')
  })

  it('borradorDe toma los datos guardados; los opcionales nulos quedan vacíos', () => {
    const atractivo = atractivoGestion({ normasAmbientales: null, horarioAtencion: null }) as AtractivoGestion
    expect(borradorDe(atractivo)).toMatchObject({
      nombre: 'Cascada El Salto',
      latitud: '11.15',
      longitud: '-74.11',
      limiteCapacidadCarga: '80',
      normasAmbientales: '',
      horarioAtencion: '',
      condicionesAccesibilidad: ['MovilidadReducida'],
    })
  })

  it('rutasConControl: las imágenes solo en el alta (en la edición van en solo lectura, DEC-226)', () => {
    expect(rutasConControl(true, 2)).toEqual(expect.arrayContaining(['estadoOperativo', 'imagenes', 'imagenes[0]', 'imagenes[1]']))
    expect(rutasConControl(false)).not.toContain('imagenes')
    expect(rutasConControl(false)).not.toContain('estadoOperativo')
  })
})
