import { describe, expect, it } from 'vitest'
import { consultaApi, contarMasFiltros, soloDeCategoria, type CriteriosBusqueda } from '../catalogoApi'
import { escribirCriterios, leerCriterios, sinMasFiltrosDescartados } from '../criteriosUrl'

const leer = (consulta: string) => leerCriterios(new URLSearchParams(consulta))

describe('«Más filtros» en la URL y en la API', () => {
  it('lee los filtros de la categoría de la URL', () => {
    expect(leer('categoria=Alojamiento&serviciosIncluidos=Piscina&serviciosIncluidos=WiFi&huespedes=3&tipoHabitacion=Caba%C3%B1a')
      .masFiltros).toEqual({ serviciosIncluidos: ['WiFi', 'Piscina'], huespedes: 3, tipoHabitacion: 'Cabaña' })
    expect(leer('categoria=Gastronomia&menuVegetariano=true&tipoCocina=+T%C3%ADpica+').masFiltros).toEqual({
      menuVegetariano: true,
      tipoCocina: 'Típica',
    })
    expect(leer('categoria=Tour&nivelDificultad=F%C3%A1cil&duracionMaxHoras=5.5').masFiltros).toEqual({
      nivelDificultad: 'Fácil',
      duracionMaxHoras: 5.5,
    })
    expect(leer('categoria=AlquilerEquipo&tipoVehiculoEquipo=Kayak+doble').masFiltros).toEqual({
      tipoVehiculoEquipo: 'Kayak doble',
    })
  })

  it.each([
    ['otra categoría', 'categoria=Gastronomia&serviciosIncluidos=WiFi&nivelDificultad=Alta'],
    ['sin categoría', 'huespedes=2&tipoCocina=Mariscos&tipoVehiculoEquipo=Kayak'],
    ['categoría inválida', 'categoria=Hoteles&huespedes=2'],
  ])('descarta en silencio los filtros con %s', (_caso, consulta) => {
    expect(leer(consulta).masFiltros).toEqual({})
  })

  it('descarta los valores inválidos', () => {
    expect(
      leer('categoria=Alojamiento&serviciosIncluidos=Jacuzzi&serviciosIncluidos=wifi&huespedes=0&tipoHabitacion=+').masFiltros,
    ).toEqual({})
    expect(leer('categoria=Alojamiento&huespedes=2.5').masFiltros).toEqual({})
    expect(leer('categoria=Gastronomia&menuVegetariano=false').masFiltros).toEqual({})
    expect(leer('categoria=Tour&duracionMaxHoras=-1').masFiltros).toEqual({})
    expect(leer(`categoria=Tour&nivelDificultad=${'a'.repeat(150)}`).masFiltros).toEqual({ nivelDificultad: 'a'.repeat(100) })
  })

  it('escribe la URL con los mismos nombres de la API, después de la categoría', () => {
    const criterios: CriteriosBusqueda = {
      texto: 'mar',
      categoria: 'Alojamiento',
      masFiltros: { serviciosIncluidos: ['WiFi', 'Desayuno'], huespedes: 2, tipoHabitacion: 'Doble' },
      accesibilidad: ['Visual'],
      pagina: 2,
    }

    expect(escribirCriterios(criterios).toString()).toBe(
      'texto=mar&categoria=Alojamiento&serviciosIncluidos=WiFi&serviciosIncluidos=Desayuno&huespedes=2&tipoHabitacion=Doble&accesibilidad=Visual&pagina=2',
    )
    expect(leerCriterios(escribirCriterios(criterios))).toEqual({ ...criterios, precioMax: undefined, distanciaKm: undefined, orden: undefined })
  })

  it('la API nunca recibe filtros de otra categoría ni sin categoría', () => {
    const base: CriteriosBusqueda = { accesibilidad: [], pagina: 1 }

    expect(consultaApi({ ...base, categoria: 'Tour', masFiltros: { tipoCocina: 'Mariscos', duracionMaxHoras: 6 } })).toBe(
      'categoria=Tour&duracionMaxHoras=6&pagina=0&tamano=10',
    )
    expect(consultaApi({ ...base, masFiltros: { huespedes: 2, menuVegetariano: true } })).toBe('pagina=0&tamano=10')
    expect(consultaApi({ ...base, categoria: 'Gastronomia', masFiltros: { menuVegetariano: false } })).toBe(
      'categoria=Gastronomia&pagina=0&tamano=10',
    )
  })

  it('cuenta y recorta los filtros por categoría', () => {
    expect(contarMasFiltros({ serviciosIncluidos: ['WiFi', 'Piscina'], huespedes: 2 })).toBe(3)
    expect(contarMasFiltros({ menuVegetariano: false })).toBe(0)
    expect(soloDeCategoria({ huespedes: 2, tipoCocina: 'Mariscos' }, 'Gastronomia')).toEqual({ tipoCocina: 'Mariscos' })
    expect(soloDeCategoria({ huespedes: 2 }, undefined)).toEqual({})
  })

  describe('sinMasFiltrosDescartados', () => {
    it('no cambia una URL válida', () => {
      expect(sinMasFiltrosDescartados(new URLSearchParams('categoria=Tour&nivelDificultad=Media&pagina=2'))).toBeNull()
      expect(sinMasFiltrosDescartados(new URLSearchParams('texto=mar'))).toBeNull()
    })

    it('quita los descartados y conserva los demás parámetros', () => {
      expect(
        sinMasFiltrosDescartados(
          new URLSearchParams('texto=mar&categoria=Tour&huespedes=2&nivelDificultad=Media&orden=Nombre'),
        )?.toString(),
      ).toBe('texto=mar&categoria=Tour&orden=Nombre&nivelDificultad=Media')
      expect(sinMasFiltrosDescartados(new URLSearchParams('menuVegetariano=true&pagina=3'))?.toString()).toBe('pagina=3')
    })
  })
})
