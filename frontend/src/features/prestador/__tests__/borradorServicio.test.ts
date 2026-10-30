import { describe, expect, it } from 'vitest'
import {
  borradorVacio,
  construirCuerpo,
  ordenarSalidas,
  precioDesde,
  rutasConControl,
  validarBorrador,
  type BorradorServicio,
} from '../borradorServicio'

/** Los datos comunes ya válidos, con la categoría que pida la prueba. */
function comunes(cambios: Partial<BorradorServicio> = {}): BorradorServicio {
  return {
    ...borradorVacio(),
    nombreComercial: '  Tour de Café en Minca ',
    descripcion: 'Recorrido guiado por fincas cafeteras.',
    telefono: '+57 300 000 0301',
    correo: 'tours@cafeminca.test',
    direccion: 'Iglesia de Minca',
    latitud: '11.1432',
    longitud: '-74.1121',
    ...cambios,
  }
}

function tour(): BorradorServicio {
  const base = comunes({ categoria: 'Tour', precioBase: '80000' })
  return {
    ...base,
    tour: {
      ...base.tour,
      puntoEncuentro: 'Iglesia de Minca',
      duracionHoras: '4,5',
      aforoMaxGrupo: '12',
      diasOperacion: ['Sabado', 'Lunes'],
      salidas: [
        { clave: 1, hora: '13:30' },
        { clave: 2, hora: '07:00' },
      ],
    },
  }
}

function alojamiento(): BorradorServicio {
  const base = comunes({ categoria: 'Alojamiento', precioBase: '99999' })
  const unidad = { nombre: 'Doble', tipoHabitacion: 'Doble', numHabitaciones: '3', capacidadMaxHuespedes: '2' }
  return {
    ...base,
    alojamiento: {
      serviciosIncluidos: ['Piscina', 'WiFi'],
      unidades: [
        { ...unidad, clave: 1, precioNoche: '150000' },
        { ...unidad, clave: 2, nombre: 'Suite', precioNoche: '120000.50' },
      ],
      diasOperacion: ['Viernes', 'Sabado'],
    },
  }
}

describe('construirCuerpo (DEC-163)', () => {
  it('Tour: oferta con días en orden de la semana y salidas, coordenadas {longitud, latitud} y textos recortados', () => {
    const cuerpo = construirCuerpo(ordenarSalidas(tour()))

    expect(cuerpo).toEqual({
      nombreComercial: 'Tour de Café en Minca',
      categoria: 'Tour',
      descripcion: 'Recorrido guiado por fincas cafeteras.',
      direccion: 'Iglesia de Minca',
      coordenadas: { longitud: -74.1121, latitud: 11.1432 },
      contacto: { telefono: '+57 300 000 0301', correo: 'tours@cafeminca.test' },
      condicionesAccesibilidad: [],
      precioBase: 80000,
      detalleCategoria: { puntoEncuentro: 'Iglesia de Minca', duracionHoras: 4.5, nivelDificultad: undefined, aforoMaxGrupo: 12 },
      oferta: { diasOperacion: ['Lunes', 'Sabado'], salidas: ['07:00', '13:30'] },
    })
    expect(Object.keys(cuerpo.coordenadas as object)).toEqual(['longitud', 'latitud'])
    // Lo que fija el servidor nunca viaja (DEC-141).
    for (const campo of ['rnt', 'estadoPublicacion', 'prestadorId', 'prestadorActivo', 'horarioAtencion']) {
      expect(cuerpo).not.toHaveProperty(campo)
    }
  })

  it('Alojamiento: sin precioBase (derivado, DEC-127), con unidades y días, sin salidas', () => {
    const cuerpo = construirCuerpo(alojamiento())

    expect(cuerpo).not.toHaveProperty('precioBase')
    expect(cuerpo.oferta).toEqual({ diasOperacion: ['Viernes', 'Sabado'] })
    expect(cuerpo.detalleCategoria).toEqual({
      serviciosIncluidos: ['WiFi', 'Piscina'],
      unidades: [
        { nombre: 'Doble', tipoHabitacion: 'Doble', numHabitaciones: 3, capacidadMaxHuespedes: 2, precioNoche: 150000 },
        { nombre: 'Suite', tipoHabitacion: 'Doble', numHabitaciones: 3, capacidadMaxHuespedes: 2, precioNoche: 120000.5 },
      ],
    })
    expect(JSON.stringify(cuerpo.detalleCategoria)).not.toContain('idUnidad')
  })

  it.each([
    ['Gastronomia', { tipoCocina: 'Caribeña', aforoMaxTurno: 40, menuVegetariano: true, rangoPrecioPlato: '$30.000 – $60.000' }],
    ['AlquilerEquipo', { tipoVehiculoEquipo: 'Kayak', unidadesDisponibles: 6, condicionesAlquiler: undefined }],
  ] as const)('%s: sin oferta (DEC-136) y solo los campos de su categoría', (categoria, detalle) => {
    const borrador = {
      ...tour(),
      categoria,
      gastronomia: { tipoCocina: 'Caribeña', aforoMaxTurno: '40', menuVegetariano: true, rangoPrecioPlato: '$30.000 – $60.000' },
      alquiler: { tipoVehiculoEquipo: 'Kayak', unidadesDisponibles: '6', condicionesAlquiler: '  ' },
    } satisfies BorradorServicio

    const cuerpo = construirCuerpo(borrador)
    expect(cuerpo).not.toHaveProperty('oferta')
    expect(cuerpo.detalleCategoria).toEqual(detalle)
    expect(cuerpo.precioBase).toBe(80000)
  })

  it('con horario lo envía recortado; vacío no lo envía', () => {
    expect(construirCuerpo({ ...tour(), horarioAtencion: '  Todos los días ' }).horarioAtencion).toBe('Todos los días')
    expect(construirCuerpo({ ...tour(), horarioAtencion: '   ' })).not.toHaveProperty('horarioAtencion')
  })
})

describe('validarBorrador (DEC-166)', () => {
  it('un borrador vacío marca los obligatorios comunes y la categoría', () => {
    expect(validarBorrador(borradorVacio())).toEqual({
      nombreComercial: 'REQUERIDO',
      categoria: 'REQUERIDO',
      descripcion: 'REQUERIDO',
      'contacto.telefono': 'REQUERIDO',
      'contacto.correo': 'REQUERIDO',
      direccion: 'REQUERIDO',
      'coordenadas.latitud': 'REQUERIDO',
      'coordenadas.longitud': 'REQUERIDO',
    })
  })

  it('un tour y un alojamiento completos son válidos', () => {
    expect(validarBorrador(tour())).toEqual({})
    expect(validarBorrador(alojamiento())).toEqual({})
  })

  it('formatos básicos, longitudes de texto en puntos de código y coordenadas fuera de rango', () => {
    const errores = validarBorrador({
      ...tour(),
      nombreComercial: '🌊'.repeat(151),
      correo: 'sin-arroba',
      latitud: '95',
      longitud: 'oeste',
      precioBase: '0',
      tour: { ...tour().tour, aforoMaxGrupo: '2.5', diasOperacion: [] },
    })
    expect(errores).toEqual({
      nombreComercial: 'LONGITUD_MAXIMA',
      'contacto.correo': 'FORMATO',
      'coordenadas.latitud': 'VALOR_MAXIMO',
      'coordenadas.longitud': 'FORMATO',
      precioBase: 'VALOR_MINIMO',
      'detalleCategoria.aforoMaxGrupo': 'FORMATO',
      'oferta.diasOperacion': 'REQUERIDO',
    })
    // 150 emojis caben: se cuentan en puntos de código, como ms-catalog.
    expect(validarBorrador({ ...tour(), nombreComercial: '🌊'.repeat(150) })).toEqual({})
  })

  it('los rangos numéricos superiores quedan para el servidor', () => {
    expect(validarBorrador({ ...tour(), tour: { ...tour().tour, aforoMaxGrupo: '999999' } })).toEqual({})
  })

  it('salidas: vacía, mal formada y duplicada, con el índice de la fila ordenada', () => {
    const borrador = ordenarSalidas({
      ...tour(),
      tour: {
        ...tour().tour,
        salidas: [
          { clave: 1, hora: '09:00' },
          { clave: 2, hora: '' },
          { clave: 3, hora: '07:00' },
          { clave: 4, hora: '09:00' },
        ],
      },
    })
    expect(borrador.tour.salidas.map((salida) => salida.hora)).toEqual(['07:00', '09:00', '09:00', ''])
    expect(validarBorrador(borrador)).toEqual({ 'oferta.salidas[2]': 'DUPLICADO', 'oferta.salidas[3]': 'REQUERIDO' })
  })

  it('unidades: cada error lleva el índice de su unidad', () => {
    const base = alojamiento()
    const errores = validarBorrador({
      ...base,
      alojamiento: {
        ...base.alojamiento,
        unidades: [base.alojamiento.unidades[0], { ...base.alojamiento.unidades[1], nombre: '', precioNoche: 'gratis' }],
      },
    })
    expect(errores).toEqual({
      'detalleCategoria.unidades[1].nombre': 'REQUERIDO',
      'detalleCategoria.unidades[1].precioNoche': 'FORMATO',
    })
  })
})

describe('precioDesde y rutasConControl', () => {
  it('el precio «desde» es el menor precio por noche válido; sin ninguno, null', () => {
    expect(precioDesde(alojamiento().alojamiento.unidades)).toBe(120000.5)
    expect(precioDesde([{ ...alojamiento().alojamiento.unidades[0], precioNoche: '' }])).toBeNull()
  })

  it('las rutas con control dependen de la categoría y de las filas', () => {
    const rutasTour = rutasConControl(tour())
    expect(rutasTour).toContain('oferta.salidas[1]')
    expect(rutasTour).not.toContain('oferta.salidas[2]')
    expect(rutasTour).not.toContain('detalleCategoria.unidades[0].precioNoche')
    expect(rutasConControl(alojamiento())).toContain('detalleCategoria.unidades[1].precioNoche')
    expect(rutasConControl(alojamiento())).not.toContain('precioBase')
    expect(rutasConControl({ ...tour(), categoria: 'Gastronomia' })).not.toContain('oferta')
  })
})
