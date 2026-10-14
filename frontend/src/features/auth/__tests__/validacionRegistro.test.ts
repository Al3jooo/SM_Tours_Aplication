import { describe, expect, it } from 'vitest'
import type { DatosRegistroPrestador, DatosRegistroTurista } from '../authApi'
import { nombresPaises } from '../registro/paises'
import { erroresDelServidor, normalizarCorreo, validarPrestador, validarTurista } from '../registro/validacionRegistro'

const TURISTA: DatosRegistroTurista = {
  primerNombre: 'Ana',
  segundoNombre: '',
  primerApellido: 'Pérez',
  segundoApellido: '',
  correo: 'ana@x.com',
  contrasena: 'Clave1234',
  paisOrigen: 'CO',
  categoriasInteres: ['Cultura'],
  aceptoPoliticas: true,
}

const PRESTADOR: DatosRegistroPrestador = {
  nombreRepresentanteLegal: 'Carlos Gómez',
  rnt: 'RNT-1',
  correoCorporativo: 'c@empresa.co',
  telefono: '+57 3000000000',
  contrasena: 'Clave1234',
  aceptoPoliticas: true,
}

/** Mismos criterios y códigos que ms-auth (RegistroTurista, RegistroPrestador y sus objetos de valor). */
describe('Validación del registro en el cliente', () => {
  it('un turista válido no tiene errores', () => {
    expect(validarTurista(TURISTA, ['CO', 'ES'])).toEqual({})
  })

  it('devuelve un código por campo con las claves JSON de la API', () => {
    expect(
      validarTurista(
        { ...TURISTA, primerNombre: ' ', primerApellido: 'x'.repeat(51), correo: 'sin-arroba', contrasena: 'corta',
          paisOrigen: 'COL', categoriasInteres: [], aceptoPoliticas: false },
        ['CO'],
      ),
    ).toEqual({
      primerNombre: 'REQUERIDO',
      primerApellido: 'LONGITUD_MAXIMA',
      correo: 'FORMATO',
      contrasena: 'LONGITUD_MINIMA',
      paisOrigen: 'FORMATO',
      categoriasInteres: 'REQUERIDO',
      aceptoPoliticas: 'POLITICAS_NO_ACEPTADAS',
    })
  })

  it('las longitudes máximas son las de V1 y se cuentan en caracteres', () => {
    expect(validarTurista({ ...TURISTA, segundoNombre: 'ñ'.repeat(50) }, null)).toEqual({})
    expect(validarTurista({ ...TURISTA, segundoNombre: 'ñ'.repeat(51) }, null)).toEqual({ segundoNombre: 'LONGITUD_MAXIMA' })
    expect(validarPrestador({ ...PRESTADOR, telefono: '1'.repeat(21), rnt: 'r'.repeat(51) })).toEqual({
      rnt: 'LONGITUD_MAXIMA',
      telefono: 'LONGITUD_MAXIMA',
    })
    expect(validarTurista({ ...TURISTA, correo: `${'a'.repeat(95)}@x.co` }, null)).toEqual({})
    expect(validarTurista({ ...TURISTA, correo: `${'a'.repeat(96)}@x.co` }, null)).toEqual({ correo: 'LONGITUD_MAXIMA' })
  })

  it('la contraseña va de 8 caracteres a 72 bytes (límite de BCrypt)', () => {
    expect(validarTurista({ ...TURISTA, contrasena: '1234567' }, null)).toEqual({ contrasena: 'LONGITUD_MINIMA' })
    expect(validarTurista({ ...TURISTA, contrasena: 'ñ'.repeat(36) }, null)).toEqual({})
    expect(validarTurista({ ...TURISTA, contrasena: 'ñ'.repeat(37) }, null)).toEqual({ contrasena: 'LONGITUD_MAXIMA' })
  })

  it('el país se valida contra la lista del backend cuando ya cargó', () => {
    expect(validarTurista({ ...TURISTA, paisOrigen: 'XX' }, ['CO'])).toEqual({ paisOrigen: 'NO_PERMITIDO' })
    expect(validarTurista({ ...TURISTA, paisOrigen: 'co' }, ['CO'])).toEqual({})
    expect(validarTurista({ ...TURISTA, paisOrigen: '' }, null)).toEqual({ paisOrigen: 'REQUERIDO' })
  })

  it('decisión D1: categorías del vocabulario, al menos una y sin repetir', () => {
    expect(validarTurista({ ...TURISTA, categoriasInteres: ['Cultura', 'Cultura'] }, null)).toEqual({
      categoriasInteres: 'DUPLICADO',
    })
    expect(validarTurista({ ...TURISTA, categoriasInteres: ['Playa'] }, null)).toEqual({
      categoriasInteres: 'NO_PERMITIDO',
    })
  })

  it('el prestador exige aceptar las políticas, igual que el turista', () => {
    expect(validarPrestador(PRESTADOR)).toEqual({})
    expect(validarPrestador({ ...PRESTADOR, aceptoPoliticas: false })).toEqual({ aceptoPoliticas: 'POLITICAS_NO_ACEPTADAS' })
  })

  it('normaliza el correo como el backend', () => {
    expect(normalizarCorreo('  Ana@X.COM ')).toBe('ana@x.com')
  })

  it('del servidor solo toma códigos conocidos, nunca textos', () => {
    expect(erroresDelServidor({ correo: 'FORMATO', rnt: 'texto libre', otro: 3 })).toEqual({ correo: 'FORMATO' })
    expect(erroresDelServidor(undefined)).toEqual({})
  })
})

describe('Nombres de los países (decisión D5)', () => {
  it('ordena por el nombre traducido, no por el código', () => {
    const codigos = ['US', 'ES', 'DE', 'CO']

    expect(nombresPaises(codigos, 'es').map((p) => p.nombre)).toEqual([
      'Alemania',
      'Colombia',
      'España',
      'Estados Unidos',
    ])
    expect(nombresPaises(codigos, 'en').map((p) => p.codigo)).toEqual(['CO', 'DE', 'ES', 'US'])
    expect(nombresPaises(codigos, 'en').map((p) => p.nombre)).toEqual(['Colombia', 'Germany', 'Spain', 'United States'])
  })

  it('sin nombre para un código, muestra el código', () => {
    expect(nombresPaises(['AA'], 'es')).toEqual([{ codigo: 'AA', nombre: 'AA' }])
  })
})
