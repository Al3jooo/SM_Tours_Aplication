import { afterEach, describe, expect, it } from 'vitest'
import { formatearRango, partesDia } from '../fechasEvento'

/** Variables de entorno de Node (Vitest), sin agregar los tipos de Node al tsconfig de la app. */
const entorno = (globalThis as unknown as { process: { env: Record<string, string | undefined> } }).process.env
const ZONA_ORIGINAL = entorno.TZ

/** Los espacios finos de Intl (U+2009, U+202F) como espacio normal, para comparar el texto. */
const plano = (texto: string) => texto.replace(/\s+/g, ' ')

describe('Fechas de día de los eventos (DEC-250)', () => {
  afterEach(() => {
    if (ZONA_ORIGINAL === undefined) delete entorno.TZ
    else entorno.TZ = ZONA_ORIGINAL
  })

  it('un evento de un día muestra una sola fecha', () => {
    expect(plano(formatearRango('2026-11-20', '2026-11-20', 'es'))).toBe('20 de nov de 2026')
    expect(plano(formatearRango('2026-11-20', '2026-11-20', 'en'))).toBe('Nov 20, 2026')
  })

  it('un rango en el mismo mes, entre meses y entre años', () => {
    expect(plano(formatearRango('2026-11-20', '2026-11-22', 'es'))).toBe('20 a 22 de nov de 2026')
    expect(plano(formatearRango('2026-11-20', '2026-11-22', 'en'))).toBe('Nov 20 – 22, 2026')
    expect(plano(formatearRango('2026-11-28', '2026-12-02', 'es'))).toBe('28 de nov al 2 de dic de 2026')
    expect(plano(formatearRango('2026-12-30', '2027-01-02', 'en'))).toBe('Dec 30, 2026 – Jan 2, 2027')
  })

  it.each([
    ['America/Los_Angeles', 480],
    ['Pacific/Kiritimati', -840],
  ])('en %s el día no se corre: «2026-11-20» sigue siendo el 20', (zona, desplazamiento) => {
    entorno.TZ = zona
    expect(new Date('2026-11-20T00:00:00Z').getTimezoneOffset()).toBe(desplazamiento)

    expect(plano(formatearRango('2026-11-20', '2026-11-20', 'es'))).toBe('20 de nov de 2026')
    expect(partesDia('2026-11-20', 'es')).toEqual({ dia: '20', mes: 'nov' })
    expect(partesDia('2026-11-01', 'en')).toEqual({ dia: '1', mes: 'Nov' })
  })
})
