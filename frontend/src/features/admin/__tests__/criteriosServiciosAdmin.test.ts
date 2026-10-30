import { describe, expect, it } from 'vitest'
import { escribirCriteriosServiciosAdmin, leerCriteriosServiciosAdmin } from '../servicios/criteriosServiciosAdmin'

const leer = (consulta: string) => leerCriteriosServiciosAdmin(new URLSearchParams(consulta))

describe('Criterios de la cola de WF-19 (DEC-175)', () => {
  it('sin parámetros: Pendiente y página 1', () => {
    expect(leer('')).toEqual({ estado: 'Pendiente', pagina: 1 })
  })

  it('lee un estado válido y una página desde 1', () => {
    expect(leer('estado=Rechazado&pagina=3')).toEqual({ estado: 'Rechazado', pagina: 3 })
  })

  it.each(['estado=Todos', 'estado=', 'estado=pendiente', 'pagina=0', 'pagina=-1', 'pagina=1.5', 'pagina=x'])(
    'un valor no válido (%s) se lee como el de por defecto',
    (consulta) => {
      expect(leer(consulta)).toEqual({ estado: 'Pendiente', pagina: 1 })
    },
  )

  it('no escribe los valores por defecto', () => {
    expect(escribirCriteriosServiciosAdmin({ estado: 'Pendiente', pagina: 1 }).toString()).toBe('')
    expect(escribirCriteriosServiciosAdmin({ estado: 'Publicado', pagina: 2 }).toString()).toBe('estado=Publicado&pagina=2')
  })
})
