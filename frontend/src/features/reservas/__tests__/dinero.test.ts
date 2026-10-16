import { describe, expect, it } from 'vitest'
import { centavos, decimalExacto, formatearCentavos, totalAlojamiento, totalTour } from '../dinero'
import { cuerpoReserva } from '../reservasApi'

describe('Dinero en centavos enteros (ajuste 6, cambio A)', () => {
  it('convierte el texto decimal o el número del catálogo a centavos, sin coma flotante', () => {
    expect(centavos('260000.00')).toBe(26_000_000)
    expect(centavos('260000.5')).toBe(26_000_050)
    expect(centavos('260000.50')).toBe(26_000_050)
    expect(centavos(150000)).toBe(15_000_000)
    // Un número con 2 decimales se valida desde su texto.
    expect(centavos(0.1)).toBe(10)
    expect(centavos(260000.5)).toBe(26_000_050)
  })

  it.each([['abc'], ['1.234'], [-5], ['-5'], [1e21], [260000.123], [''], ['1,5'], [' 100'], [null], [undefined]])(
    'rechaza %s (formato inválido): sin total',
    (valor) => {
      expect(centavos(valor as never)).toBeNull()
    },
  )

  it('rechaza lo que no cabe en un entero seguro', () => {
    expect(centavos('90071992547409.93')).toBeNull()
    expect(totalAlojamiento(Number.MAX_SAFE_INTEGER, 2, 1)).toBeNull()
  })

  it('el total se arma multiplicando enteros: 260000.50 × 3 noches × 2 habitaciones = 1560003.00', () => {
    const total = totalAlojamiento(centavos('260000.50'), 3, 2)
    expect(total).toBe(156_000_300)
    expect(decimalExacto(total!)).toBe('1560003.00')
    expect(totalTour(centavos(80000), 3)).toBe(24_000_000)
    expect(totalTour(null, 3)).toBeNull()
    expect(totalAlojamiento(centavos(1), 0, 1)).toBeNull()
  })

  it('el texto decimal exacto siempre lleva 2 decimales', () => {
    expect(decimalExacto(26_000_000)).toBe('260000.00')
    expect(decimalExacto(5)).toBe('0.05')
    expect(decimalExacto(0)).toBe('0.00')
  })

  it('se muestra en COP con decimales solo si no son cero', () => {
    expect(formatearCentavos(26_000_000, 'es')).toBe(new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(260000))
    expect(formatearCentavos(26_000_000, 'es')).not.toMatch(/,00/)
    // 1560003.00 no tiene centavos: sin decimales; 260000.50 sí los tiene.
    expect(formatearCentavos(156_000_300, 'es')).toMatch(/1\.560\.003$/)
    expect(formatearCentavos(26_000_050, 'es')).toMatch(/260\.000,50$/)
    expect(formatearCentavos(26_000_050, 'en')).toMatch(/260,000\.50$/)
  })

  it('el cuerpo de la reserva lleva precioTotalEsperado como número decimal exacto, sin comillas', () => {
    const cuerpo = cuerpoReserva({
      categoria: 'Alojamiento',
      idServicio: 's-hotel',
      idUnidad: 'u-suite',
      fechaEntrada: '2026-12-12',
      fechaSalida: '2026-12-15',
      cantidad: 2,
      personas: 5,
      totalCentavos: 156_000_300,
    })
    expect(cuerpo).toBe(
      '{"idServicio":"s-hotel","idUnidad":"u-suite","fechaEntrada":"2026-12-12","fechaSalida":"2026-12-15",' +
        '"cantidad":2,"personas":5,"precioTotalEsperado":1560003.00}',
    )
    expect(JSON.parse(cuerpo).precioTotalEsperado).toBe(1560003)

    expect(
      cuerpoReserva({
        categoria: 'Tour',
        idServicio: 's-tour-cafe',
        fechaEntrada: '2026-12-13',
        horaInicio: '08:00',
        personas: 2,
        totalCentavos: 16_000_000,
      }),
    ).toBe('{"idServicio":"s-tour-cafe","fechaEntrada":"2026-12-13","horaInicio":"08:00","personas":2,"precioTotalEsperado":160000.00}')
  })
})
