import { describe, expect, it } from 'vitest'
import { escribirCriteriosAdmin, leerCriteriosAdmin, MAXIMO_BUSQUEDA } from '../criteriosAdmin'

const leer = (consulta: string) => leerCriteriosAdmin(new URLSearchParams(consulta))

describe('criterios de WF-12 en la URL (DEC-51)', () => {
  it('sin parámetros: pestaña «pendientes», sin estado ni búsqueda, página 1', () => {
    expect(leer('')).toEqual({ pestana: 'pendientes', estado: '', q: '', pagina: 1 })
  })

  it('lee pestaña, estado, búsqueda y página', () => {
    expect(leer('pestana=turistas&estado=Desactivado&q=marta&pagina=3')).toEqual({
      pestana: 'turistas',
      estado: 'Desactivado',
      q: 'marta',
      pagina: 3,
    })
  })

  it('descarta valores inválidos: pestaña desconocida, estado de otra pestaña y página no entera', () => {
    expect(leer('pestana=gestores')).toMatchObject({ pestana: 'pendientes' })
    expect(leer('pestana=turistas&estado=Inactivo')).toMatchObject({ estado: '' })
    expect(leer('pestana=prestadores&estado=Desactivado')).toMatchObject({ estado: '' })
    // La pestaña «pendientes» ya fija Pendiente: no admite filtro de estado.
    expect(leer('estado=Activo')).toMatchObject({ estado: '' })
    for (const pagina of ['0', '-1', '1.5', 'x']) expect(leer(`pagina=${pagina}`).pagina).toBe(1)
  })

  it('recorta la búsqueda al máximo de ms-auth', () => {
    expect(leer(`q=${'a'.repeat(150)}`).q).toHaveLength(MAXIMO_BUSQUEDA)
  })

  it('escribe solo lo que no es por defecto, en un orden fijo', () => {
    expect(escribirCriteriosAdmin({ pestana: 'pendientes', estado: '', q: '', pagina: 1 }).toString()).toBe('')
    expect(escribirCriteriosAdmin({ pestana: 'prestadores', estado: 'Activo', q: ' ana ', pagina: 2 }).toString()).toBe(
      'pestana=prestadores&estado=Activo&q=ana&pagina=2',
    )
  })

  it('leer y escribir son inversos', () => {
    const consulta = 'pestana=turistas&estado=Activo&q=pablo&pagina=4'
    expect(escribirCriteriosAdmin(leer(consulta)).toString()).toBe(consulta)
  })
})
