import { afterEach, describe, expect, it } from 'vitest'
import {
  diasEntre,
  esFechaIso,
  fechasDelRango,
  formatearFecha,
  formatearHora,
  formatearInstante,
  hoyBogota,
  sumarDias,
  ventana,
} from '../fechasBogota'

/** Variables de entorno de Node (Vitest), sin agregar los tipos de Node al tsconfig de la app. */
const entorno = (globalThis as unknown as { process: { env: Record<string, string | undefined> } }).process.env
const ZONA_ORIGINAL = entorno.TZ

/** Cambia la zona horaria del «navegador» (Node la respeta en caliente) y comprueba que el cambio aplicó. */
function enZona(zona: string, desplazamientoMinutos: number) {
  entorno.TZ = zona
  expect(new Date('2026-10-17T03:30:00Z').getTimezoneOffset()).toBe(desplazamientoMinutos)
}

describe('Fechas de Santa Marta (America/Bogota)', () => {
  afterEach(() => {
    if (ZONA_ORIGINAL === undefined) delete entorno.TZ
    else entorno.TZ = ZONA_ORIGINAL
  })

  // 03:30 UTC del 17 son las 22:30 del 16 en Santa Marta (UTC−5): ya es 17 en UTC, pero no en Bogotá.
  const NOCHE_DEL_16 = new Date('2026-10-17T03:30:00Z')
  // 05:00 UTC del 17 es la medianoche del 17 en Santa Marta.
  const MEDIANOCHE_DEL_17 = new Date('2026-10-17T05:00:00Z')

  it('hoy es el día de Santa Marta, aunque en UTC ya sea el día siguiente', () => {
    expect(hoyBogota(NOCHE_DEL_16)).toBe('2026-10-16')
    expect(hoyBogota(MEDIANOCHE_DEL_17)).toBe('2026-10-17')
  })

  it.each([
    ['Pacific/Kiritimati', -840],
    ['Pacific/Honolulu', 600],
  ])('con el navegador en %s, hoy y la ventana no se corren un día', (zona, desplazamiento) => {
    enZona(zona, desplazamiento)

    expect(hoyBogota(NOCHE_DEL_16)).toBe('2026-10-16')
    expect(hoyBogota(MEDIANOCHE_DEL_17)).toBe('2026-10-17')
    const hoy = hoyBogota(NOCHE_DEL_16)
    expect(sumarDias(hoy, 1)).toBe('2026-10-17')
    expect(ventana(hoy)).toEqual({ ultimaNoche: '2026-12-14', ultimaSalida: '2026-12-15' })
    expect(formatearFecha('2026-10-16', 'es')).toMatch(/^16 de oct/)
    expect(formatearFecha('2026-10-16', 'en')).toMatch(/^Oct 16/)
    expect(formatearFecha('2026-10-16', 'es', { day: 'numeric' })).toBe('16')
    // Una hora siempre se muestra en la de Santa Marta: en español, con 24 horas.
    expect(formatearHora('2026-12-12T13:00:00Z', 'es')).toBe('08:00')
    expect(formatearHora('2026-12-12T23:00:00Z', 'es')).toBe('18:00')
    expect(formatearHora('2026-12-12T23:00:00Z', 'en')).toMatch(/^06:00\sPM$/)
    expect(formatearInstante('2026-12-11T20:00:00Z', 'es')).toMatch(/^11 de dic de 2026, 15:00$/)
  })

  it('suma días en el calendario, también al cambiar de mes y de año', () => {
    expect(sumarDias('2026-12-31', 1)).toBe('2027-01-01')
    expect(sumarDias('2026-03-01', -1)).toBe('2026-02-28')
    expect(diasEntre('2026-12-12', '2026-12-15')).toBe(3)
    expect(diasEntre('2026-12-15', '2026-12-12')).toBe(-3)
    expect(fechasDelRango('2026-12-30', '2027-01-02')).toEqual(['2026-12-30', '2026-12-31', '2027-01-01'])
    expect(fechasDelRango('2026-12-12', '2026-12-12')).toEqual([])
  })

  it('una fecha debe existir en el calendario', () => {
    expect(esFechaIso('2026-02-28')).toBe(true)
    expect(esFechaIso('2026-02-30')).toBe(false)
    expect(esFechaIso('31/12/2026')).toBe(false)
    expect(esFechaIso('')).toBe(false)
    expect(esFechaIso(undefined)).toBe(false)
  })
})
