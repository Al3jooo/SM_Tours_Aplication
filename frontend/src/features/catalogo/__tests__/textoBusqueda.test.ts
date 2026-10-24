import { describe, expect, it } from 'vitest'
import i18n from '../../../i18n'
import { criteriosHistorial } from '../../../test/fetchSimulado'
import { cuerpoHistorial, type CriteriosHistorial } from '../historialApi'
import { rutaRepetir, textoBusqueda } from '../textoBusqueda'

const guardados = (criterios: Record<string, unknown>) => criteriosHistorial(criterios) as CriteriosHistorial
const texto = (criterios: Record<string, unknown>, idioma = 'es') =>
  textoBusqueda(guardados(criterios), i18n.getFixedT(idioma), idioma).replace(/\s/g, ' ')

describe('Texto de una búsqueda reciente (RF60, DEC-96)', () => {
  it('usa las palabras de los filtros, en el orden de WF-05', () => {
    expect(
      texto({
        texto: 'playa',
        categoria: 'Alojamiento',
        serviciosIncluidos: ['WiFi', 'Desayuno'],
        huespedes: 2,
        precioMax: 200000,
        accesibilidad: ['MovilidadReducida'],
        orden: 'PrecioAscendente',
      }),
    ).toBe('“playa” · Alojamiento · WiFi · Desayuno · 2 huéspedes · Hasta $ 200.000 · Movilidad reducida · Precio: menor a mayor')
  })

  it('con ubicación: «Cerca de mí», con la distancia si la tiene; el orden por distancia no se repite', () => {
    expect(texto({ usarUbicacion: true, orden: 'Distancia' })).toBe('Cerca de mí')
    expect(texto({ usarUbicacion: true, distanciaKm: 2.5 })).toBe('Cerca de mí · 2,5 km')
    expect(texto({ texto: 'Tayrona', orden: 'Nombre' })).toBe('“Tayrona”')
  })

  it('en inglés (RNF08)', () => {
    expect(texto({ categoria: 'Tour', nivelDificultad: 'Media', usarUbicacion: true, distanciaKm: 5 }, 'en')).toBe(
      'Tours · Difficulty level: Media · Near me · 5 km',
    )
  })
})

describe('Repetir y registrar una búsqueda', () => {
  it('la ruta lleva los criterios en la URL de /explorar, sin ubicación ni página', () => {
    expect(rutaRepetir(guardados({ texto: 'playa', categoria: 'Gastronomia', menuVegetariano: true }))).toBe(
      '/explorar?texto=playa&categoria=Gastronomia&menuVegetariano=true',
    )
    expect(rutaRepetir(guardados({ usarUbicacion: true, distanciaKm: 5, orden: 'Distancia' }))).toBe(
      '/explorar?distanciaKm=5&orden=Distancia',
    )
    // Un filtro de «Más filtros» sin su categoría no viaja (la API lo rechazaría).
    expect(rutaRepetir(guardados({ huespedes: 2 }))).toBe('/explorar')
  })

  it('el cuerpo del POST no lleva distancia ni orden por distancia sin ubicación; sin criterios, no hay cuerpo', () => {
    const base = { accesibilidad: [], pagina: 3 }
    expect(cuerpoHistorial({ ...base, distanciaKm: 5, orden: 'Distancia' }, false)).toBeNull()
    expect(cuerpoHistorial({ ...base, orden: 'Nombre' }, false)).toBeNull()
    expect(cuerpoHistorial({ ...base, distanciaKm: 5, orden: 'Distancia' }, true)).toEqual({
      distanciaKm: 5,
      orden: 'Distancia',
      usarUbicacion: true,
    })
    expect(
      cuerpoHistorial(
        { ...base, texto: 'kayak', categoria: 'Tour', masFiltros: { duracionMaxHoras: 4, huespedes: 2 }, tamano: 10 },
        false,
      ),
    ).toEqual({ texto: 'kayak', categoria: 'Tour', duracionMaxHoras: 4, usarUbicacion: false })
  })
})
