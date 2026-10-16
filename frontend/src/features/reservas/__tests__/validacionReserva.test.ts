import { describe, expect, it } from 'vitest'
import {
  ajustarSalida,
  leerEntero,
  limitesSalida,
  validarBusquedaAlojamiento,
  validarBusquedaTour,
  validarHabitaciones,
  validarPersonasTour,
} from '../validacionReserva'

const HOY = '2026-10-16'
// Ventana: la última noche es hoy + 59 (14/12) y la última salida, hoy + 60 (15/12).

describe('Validación de WF-10 (mismos códigos que ms-booking)', () => {
  it('alojamiento: fechas obligatorias, pasadas, fuera de la ventana y rango', () => {
    expect(validarBusquedaAlojamiento({ entrada: '', salida: '', personas: undefined }, HOY)).toEqual({
      entrada: 'REQUERIDO',
      salida: 'REQUERIDO',
      personas: 'REQUERIDO',
    })
    expect(validarBusquedaAlojamiento({ entrada: '2026-10-15', salida: '2026-10-17', personas: 2 }, HOY)).toEqual({
      entrada: 'FECHA_PASADA',
    })
    expect(validarBusquedaAlojamiento({ entrada: '2026-12-15', salida: '2026-12-16', personas: 2 }, HOY)).toEqual({
      entrada: 'FUERA_DE_VENTANA',
    })
    expect(validarBusquedaAlojamiento({ entrada: '2026-12-12', salida: '2026-12-12', personas: 2 }, HOY)).toEqual({
      salida: 'RANGO_INVALIDO',
    })
    expect(validarBusquedaAlojamiento({ entrada: '2026-11-01', salida: '2026-11-16', personas: 2 }, HOY)).toEqual({
      salida: 'MAXIMO_NOCHES',
    })
    // 14 noches es el máximo, incluido; la última salida posible es hoy + 60.
    expect(validarBusquedaAlojamiento({ entrada: '2026-11-01', salida: '2026-11-15', personas: 2 }, HOY)).toEqual({})
    expect(validarBusquedaAlojamiento({ entrada: '2026-12-14', salida: '2026-12-15', personas: 2 }, HOY)).toEqual({})
    expect(validarBusquedaAlojamiento({ entrada: '2026-12-10', salida: '2026-12-16', personas: 2 }, HOY)).toEqual({
      salida: 'FUERA_DE_VENTANA',
    })
  })

  it('una fecha escrita a mano se valida aunque el campo tenga min y max', () => {
    expect(validarBusquedaAlojamiento({ entrada: '2026-02-30', salida: '2026-03-02', personas: 1 }, HOY).entrada).toBe('FORMATO')
    expect(validarBusquedaTour({ fecha: '13/12/2026', personas: 1 }, HOY)).toEqual({ fecha: 'FORMATO' })
  })

  it('personas: obligatorias, enteras y al menos 1', () => {
    expect(leerEntero('')).toBeUndefined()
    expect(leerEntero('2.5')).toBeNaN()
    expect(leerEntero(' 3 ')).toBe(3)
    expect(validarBusquedaTour({ fecha: HOY, personas: leerEntero('0') }, HOY)).toEqual({ personas: 'VALOR_MINIMO' })
    expect(validarBusquedaTour({ fecha: HOY, personas: leerEntero('x') }, HOY)).toEqual({ personas: 'FORMATO' })
    expect(validarBusquedaTour({ fecha: HOY, personas: 2 }, HOY)).toEqual({})
  })

  it('habitaciones: de 1 al cupo, y los huéspedes deben caber', () => {
    const suite = { capacidadMaxHuespedes: 4 }
    expect(validarHabitaciones({ habitaciones: 2, personas: 5 }, suite, 3)).toEqual({})
    expect(validarHabitaciones({ habitaciones: 4, personas: 5 }, suite, 3)).toEqual({ habitaciones: 'EXCEDE_CUPO' })
    expect(validarHabitaciones({ habitaciones: 1, personas: 5 }, suite, 3)).toEqual({ habitaciones: 'EXCEDE_CAPACIDAD' })
    expect(validarHabitaciones({ habitaciones: 0, personas: 1 }, suite, 3)).toEqual({ habitaciones: 'VALOR_MINIMO' })
    expect(validarHabitaciones({ habitaciones: undefined, personas: 1 }, suite, 3)).toEqual({ habitaciones: 'REQUERIDO' })
  })

  it('tour: personas hasta el aforo del grupo y el cupo del horario', () => {
    expect(validarPersonasTour(13, 12, 12)).toEqual({ personas: 'EXCEDE_AFORO' })
    expect(validarPersonasTour(6, 12, 5)).toEqual({ personas: 'EXCEDE_CUPO' })
    expect(validarPersonasTour(5, 12, 5)).toEqual({})
  })

  it('límites de la salida: entrada + 1 y el menor entre entrada + 14 y la última salida', () => {
    expect(limitesSalida('2026-11-01', HOY)).toEqual({ min: '2026-11-02', max: '2026-11-15' })
    expect(limitesSalida('2026-12-10', HOY)).toEqual({ min: '2026-12-11', max: '2026-12-15' })
    expect(limitesSalida('', HOY)).toBeNull()
  })

  it('al cambiar la entrada, la salida fuera de rango se ajusta a entrada + 1', () => {
    expect(ajustarSalida('2026-11-10', '2026-11-05', HOY)).toEqual({ salida: '2026-11-11', ajuste: 'ajustada' })
    expect(ajustarSalida('2026-11-01', '2026-11-20', HOY)).toEqual({ salida: '2026-11-02', ajuste: 'ajustada' })
    expect(ajustarSalida('2026-11-01', '2026-11-05', HOY)).toEqual({ salida: '2026-11-05', ajuste: null })
    expect(ajustarSalida('2026-11-01', '', HOY)).toEqual({ salida: '', ajuste: null })
    // Con la entrada fuera de la ventana no cabe ni una noche: se borra la salida.
    expect(ajustarSalida('2026-12-20', '2026-12-18', HOY)).toEqual({ salida: '', ajuste: 'borrada' })
  })
})
