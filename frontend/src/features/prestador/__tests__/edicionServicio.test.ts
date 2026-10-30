import { describe, expect, it } from 'vitest'
import { ApiError } from '../../../shared/api/httpClient'
import { servicioGestion, servicioGestionHotel } from '../../../test/fetchSimulado'
import type { ServicioGestion } from '../../servicios/servicioGestion'
import {
  borradorDatosDe,
  borradorPrecioDe,
  clasificarRechazoEdicion,
  cuerpoDatosComerciales,
  cuerpoPrecio,
  esEditable,
  fusionarPrecio,
  precioDesdeUnidades,
  rutasPrecio,
  validarDatosComerciales,
  validarPrecio,
  valoresPrecio,
} from '../edicionServicio'

/** Funciones puras de la edición del Prestador (RF32, RF43; DEC-198 a DEC-201). */

const tour = servicioGestion() as ServicioGestion
const hotel = servicioGestionHotel() as ServicioGestion

describe('Simulado (C7)', () => {
  it('la forma de gestión trae la `version` (DEC-186, DEC-205)', () => {
    expect(servicioGestion()).toHaveProperty('version', 0)
    expect(servicioGestionHotel({ version: 4 })).toHaveProperty('version', 4)
  })
})

describe('Estados editables (DEC-185)', () => {
  it('solo Pendiente y Publicado', () => {
    expect(esEditable('Pendiente')).toBe(true)
    expect(esEditable('Publicado')).toBe(true)
    expect(esEditable('Rechazado')).toBe(false)
  })
})

describe('Precio (RF32)', () => {
  it('Tour: solo `precioBase`; el texto con coma decimal se envía como número', () => {
    const borrador = { ...borradorPrecioDe(tour), precioBase: '95000,50' }
    expect(cuerpoPrecio(tour, borrador)).toEqual({ precioBase: 95000.5 })
    expect(rutasPrecio(tour, borrador)).toEqual(['precioBase'])
  })

  it('Alojamiento: todas las unidades, una vez cada una, con su id; nunca `precioBase`', () => {
    const borrador = borradorPrecioDe(hotel)
    expect(borrador.unidades.map((u) => [u.idUnidad, u.precioNoche])).toEqual([
      ['u-doble', '150000'],
      ['u-suite', '260000.50'],
    ])
    borrador.unidades[1].precioNoche = '240000'
    expect(cuerpoPrecio(hotel, borrador)).toEqual({
      unidades: [
        { idUnidad: 'u-doble', precioNoche: 150000 },
        { idUnidad: 'u-suite', precioNoche: 240000 },
      ],
    })
    expect(rutasPrecio(hotel, borrador)).toEqual(['unidades', 'unidades[0].precioNoche', 'unidades[1].precioNoche'])
  })

  it('el «desde» es el menor precio por noche válido', () => {
    const borrador = borradorPrecioDe(hotel)
    expect(precioDesdeUnidades(borrador.unidades)).toBe(150000)
    borrador.unidades[1].precioNoche = '90000'
    expect(precioDesdeUnidades(borrador.unidades)).toBe(90000)
    borrador.unidades[0].precioNoche = ''
    borrador.unidades[1].precioNoche = 'x'
    expect(precioDesdeUnidades(borrador.unidades)).toBeNull()
  })

  it('tope de 99 999 999,99 y 2 decimales como mucho (DEC-198, C10)', () => {
    const precio = (precioBase: string) => validarPrecio(tour, { precioBase, unidades: [] })
    expect(precio('99999999.99')).toEqual({})
    expect(precio('99999999,99')).toEqual({})
    expect(precio('100000000')).toEqual({ precioBase: 'VALOR_MAXIMO' })
    expect(precio('99999999.991')).toEqual({ precioBase: 'FORMATO' })
    expect(precio('80000.5')).toEqual({})
    expect(precio('80000.555')).toEqual({ precioBase: 'FORMATO' })
    expect(precio('0.01')).toEqual({})
    const borrador = borradorPrecioDe(hotel)
    borrador.unidades[0].precioNoche = '120000.123'
    borrador.unidades[1].precioNoche = '100000000'
    expect(validarPrecio(hotel, borrador)).toEqual({
      'unidades[0].precioNoche': 'FORMATO',
      'unidades[1].precioNoche': 'VALOR_MAXIMO',
    })
  })

  it('validación en el cliente: obligatorio, formato y mayor que 0, con la ruta de la unidad', () => {
    expect(validarPrecio(tour, { precioBase: '', unidades: [] })).toEqual({ precioBase: 'REQUERIDO' })
    expect(validarPrecio(tour, { precioBase: 'diez', unidades: [] })).toEqual({ precioBase: 'FORMATO' })
    expect(validarPrecio(tour, { precioBase: '0', unidades: [] })).toEqual({ precioBase: 'VALOR_MINIMO' })
    const borrador = borradorPrecioDe(hotel)
    borrador.unidades[1].precioNoche = ''
    expect(validarPrecio(hotel, borrador)).toEqual({ 'unidades[1].precioNoche': 'REQUERIDO' })
  })

  it('al releer conserva lo escrito por unidad, en el orden vigente', () => {
    const borrador = borradorPrecioDe(hotel)
    borrador.unidades[0].precioNoche = '170000'
    const vigente = servicioGestionHotel({
      detalleCategoria: {
        ...(hotel.detalleCategoria as object),
        unidades: [...((hotel.detalleCategoria as { unidades: object[] }).unidades)].reverse(),
      },
    }) as ServicioGestion
    expect(fusionarPrecio(borrador, vigente).unidades.map((u) => [u.idUnidad, u.precioNoche])).toEqual([
      ['u-suite', '260000.50'],
      ['u-doble', '170000'],
    ])
  })

  it('los valores comparables van por id de unidad', () => {
    expect(valoresPrecio(hotel)).toEqual({ 'unidad:u-doble': 150000, 'unidad:u-suite': 260000.5 })
    expect(valoresPrecio(tour)).toEqual({ precioBase: 80000 })
  })
})

describe('Datos comerciales del servicio (RF43)', () => {
  it('el cuerpo lleva solo el nombre y el contacto, recortados', () => {
    const borrador = { ...borradorDatosDe(tour), nombreComercial: '  Café Minca  ' }
    expect(cuerpoDatosComerciales(borrador)).toEqual({
      nombreComercial: 'Café Minca',
      contacto: { telefono: '+57 300 000 0301', correo: 'tours@cafeminca.test' },
    })
  })

  it('obligatorios, longitudes y forma del correo', () => {
    expect(validarDatosComerciales({ nombreComercial: ' ', telefono: '', correo: 'sin-arroba' })).toEqual({
      nombreComercial: 'REQUERIDO',
      'contacto.telefono': 'REQUERIDO',
      'contacto.correo': 'FORMATO',
    })
    expect(validarDatosComerciales({ nombreComercial: 'x'.repeat(151), telefono: '1'.repeat(21), correo: 'a@b.co' })).toEqual({
      nombreComercial: 'LONGITUD_MAXIMA',
      'contacto.telefono': 'LONGITUD_MAXIMA',
    })
  })

  it('límites de DEC-199 (C10): 150, 20 y 254 pasan; uno más no', () => {
    const correo254 = `${'a'.repeat(248)}@b.com`
    expect(correo254).toHaveLength(254)
    expect(validarDatosComerciales({ nombreComercial: 'x'.repeat(150), telefono: '1'.repeat(20), correo: correo254 })).toEqual({})
    expect(validarDatosComerciales({ nombreComercial: 'Café', telefono: '300', correo: `a${correo254}` })).toEqual({
      'contacto.correo': 'LONGITUD_MAXIMA',
    })
  })
})

describe('Rechazos de un PUT de edición (DEC-200, DEC-201)', () => {
  const rutas = ['precioBase']
  it('409 SERVICIO_MODIFICADO y ESTADO_CAMBIADO (con o sin `estadoActual`)', () => {
    expect(clasificarRechazoEdicion(new ApiError(409, 'SERVICIO_MODIFICADO', { versionActual: 3 }), rutas)).toEqual({ tipo: 'modificado' })
    expect(clasificarRechazoEdicion(new ApiError(409, 'ESTADO_CAMBIADO', { estadoActual: 'Rechazado' }), rutas)).toEqual({
      tipo: 'estadoCambiado',
      estadoActual: 'Rechazado',
    })
    expect(clasificarRechazoEdicion(new ApiError(409, 'ESTADO_CAMBIADO', null), rutas)).toEqual({ tipo: 'estadoCambiado', estadoActual: null })
  })

  it('400: los campos con control junto al campo; uno sin control, al mensaje general', () => {
    expect(clasificarRechazoEdicion(new ApiError(400, 'DATOS_INVALIDOS', { campos: { precioBase: 'VALOR_MAXIMO' } }), rutas)).toEqual({
      tipo: 'campos',
      campos: { precioBase: 'VALOR_MAXIMO' },
      general: null,
    })
    expect(
      clasificarRechazoEdicion(new ApiError(400, 'DATOS_INVALIDOS', { campos: { 'unidades[0].idUnidad': 'NO_PERMITIDO' } }), rutas),
    ).toEqual({ tipo: 'campos', campos: {}, general: 'DATOS_INVALIDOS' })
  })

  it('404, 401 y red', () => {
    expect(clasificarRechazoEdicion(new ApiError(404, 'SERVICIO_NO_ENCONTRADO', null), rutas)).toEqual({ tipo: 'noEncontrado' })
    expect(clasificarRechazoEdicion(new ApiError(401, 'SESION_INVALIDADA', null), rutas)).toEqual({ tipo: 'sesion' })
    expect(clasificarRechazoEdicion(new ApiError(0, 'ERROR_RED', null), rutas)).toEqual({ tipo: 'general', clave: 'ERROR_RED' })
  })
})
