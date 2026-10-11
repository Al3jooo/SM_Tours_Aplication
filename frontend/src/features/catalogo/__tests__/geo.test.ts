import { describe, expect, it } from 'vitest'
import { CENTRO_SANTA_MARTA, RADIO_SANTA_MARTA_KM, distanciaKm, estaLejosDeSantaMarta } from '../geo'

const BOGOTA = { latitud: 4.6097, longitud: -74.0817 }
const EL_RODADERO = { latitud: 11.2047, longitud: -74.2272 }

describe('geo', () => {
  it('la distancia del centro a sí mismo es 0', () => {
    expect(distanciaKm(CENTRO_SANTA_MARTA, CENTRO_SANTA_MARTA)).toBe(0)
  })

  it('de Bogotá a Santa Marta hay unos 700 km', () => {
    const distancia = distanciaKm(BOGOTA, CENTRO_SANTA_MARTA)
    expect(distancia).toBeGreaterThan(690)
    expect(distancia).toBeLessThan(750)
    expect(distanciaKm(CENTRO_SANTA_MARTA, BOGOTA)).toBeCloseTo(distancia, 9)
  })

  it('El Rodadero queda dentro del radio de Santa Marta', () => {
    expect(distanciaKm(EL_RODADERO, CENTRO_SANTA_MARTA)).toBeLessThan(RADIO_SANTA_MARTA_KM)
    expect(estaLejosDeSantaMarta(EL_RODADERO)).toBe(false)
  })

  it('Bogotá está lejos de Santa Marta', () => {
    expect(estaLejosDeSantaMarta(BOGOTA)).toBe(true)
  })

  it('el límite es estrictamente mayor que el radio', () => {
    // 1° de latitud ≈ 111,2 km: 0,6° al norte (≈ 66,7 km) queda fuera; 0,5° (≈ 55,6 km), dentro.
    expect(estaLejosDeSantaMarta({ ...CENTRO_SANTA_MARTA, latitud: CENTRO_SANTA_MARTA.latitud + 0.6 })).toBe(true)
    expect(estaLejosDeSantaMarta({ ...CENTRO_SANTA_MARTA, latitud: CENTRO_SANTA_MARTA.latitud + 0.5 })).toBe(false)
  })
})
