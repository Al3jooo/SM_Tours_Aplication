import { longitudTexto } from '../admin/textoAcotado'
import {
  CONDICIONES_ACCESIBILIDAD,
  SERVICIOS_INCLUIDOS,
  type Categoria,
  type CondicionAccesibilidad,
  type ServicioIncluido,
} from '../catalogo/catalogoApi'
import { LONGITUD_MAXIMA, type ErroresServicio } from './rutasCampos'

/** Días de la oferta, sin tildes, como el contrato (DEC-136). */
export const DIAS_SEMANA = ['Lunes', 'Martes', 'Miercoles', 'Jueves', 'Viernes', 'Sabado', 'Domingo'] as const
export type DiaSemana = (typeof DIAS_SEMANA)[number]

/** Topes de filas del contrato (DEC-133, DEC-136): se aplican en los botones de agregar. */
export const MAXIMO_UNIDADES = 20
export const MAXIMO_SALIDAS = 12

/** Categorías con oferta inicial (DEC-136); Gastronomía y Alquiler no la tienen. */
export const CATEGORIAS_CON_OFERTA: readonly Categoria[] = ['Tour', 'Alojamiento']

/** Los números se guardan como texto, tal como se escriben, y se convierten al armar el cuerpo. */
export interface UnidadBorrador {
  /** Solo para React: no viaja (el `idUnidad` lo genera el servidor). */
  clave: number
  nombre: string
  tipoHabitacion: string
  numHabitaciones: string
  capacidadMaxHuespedes: string
  precioNoche: string
}

export interface SalidaBorrador {
  clave: number
  hora: string
}

/**
 * Borrador de WF-08 (DEC-163). Guarda el detalle y la oferta de **cada** categoría: al cambiar de categoría y volver,
 * lo escrito sigue ahí, pero `construirCuerpo` solo envía lo de la categoría elegida.
 */
export interface BorradorServicio {
  nombreComercial: string
  categoria: Categoria | ''
  descripcion: string
  telefono: string
  correo: string
  direccion: string
  latitud: string
  longitud: string
  horarioAtencion: string
  precioBase: string
  condicionesAccesibilidad: CondicionAccesibilidad[]
  alojamiento: { serviciosIncluidos: ServicioIncluido[]; unidades: UnidadBorrador[]; diasOperacion: DiaSemana[] }
  tour: {
    puntoEncuentro: string
    duracionHoras: string
    nivelDificultad: string
    aforoMaxGrupo: string
    diasOperacion: DiaSemana[]
    salidas: SalidaBorrador[]
  }
  gastronomia: { tipoCocina: string; aforoMaxTurno: string; menuVegetariano: boolean; rangoPrecioPlato: string }
  alquiler: { tipoVehiculoEquipo: string; unidadesDisponibles: string; condicionesAlquiler: string }
}

let siguienteClave = 1
/** Clave nueva para una fila (unidad o salida). */
export function nuevaClave(): number {
  siguienteClave += 1
  return siguienteClave
}

export function unidadVacia(): UnidadBorrador {
  return { clave: nuevaClave(), nombre: '', tipoHabitacion: '', numHabitaciones: '', capacidadMaxHuespedes: '', precioNoche: '' }
}

export function salidaVacia(): SalidaBorrador {
  return { clave: nuevaClave(), hora: '' }
}

export function borradorVacio(): BorradorServicio {
  return {
    nombreComercial: '',
    categoria: '',
    descripcion: '',
    telefono: '',
    correo: '',
    direccion: '',
    latitud: '',
    longitud: '',
    horarioAtencion: '',
    precioBase: '',
    condicionesAccesibilidad: [],
    alojamiento: { serviciosIncluidos: [], unidades: [unidadVacia()], diasOperacion: [] },
    tour: { puntoEncuentro: '', duracionHoras: '', nivelDificultad: '', aforoMaxGrupo: '', diasOperacion: [], salidas: [salidaVacia()] },
    gastronomia: { tipoCocina: '', aforoMaxTurno: '', menuVegetariano: false, rangoPrecioPlato: '' },
    alquiler: { tipoVehiculoEquipo: '', unidadesDisponibles: '', condicionesAlquiler: '' },
  }
}

/** Misma forma que valida ms-catalog (DEC-138). */
const FORMA_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const FORMA_HORA = /^([01]\d|2[0-3]):[0-5]\d$/
const FORMA_DECIMAL = /^\d+([.,]\d+)?$/
const FORMA_ENTERO = /^\d+$/

/** Texto numérico → número (admite coma decimal); null si está vacío o no es un número. */
export function aNumero(texto: string): number | null {
  const limpio = texto.trim().replace(',', '.')
  if (limpio === '' || !/^-?\d+(\.\d+)?$/.test(limpio)) return null
  return Number(limpio)
}

/** Precio «desde» de un alojamiento (DEC-127): el menor `precioNoche` válido; null si no hay ninguno. */
export function precioDesde(unidades: UnidadBorrador[]): number | null {
  const precios = unidades.map((unidad) => aNumero(unidad.precioNoche)).filter((precio): precio is number => precio !== null && precio > 0)
  return precios.length === 0 ? null : Math.min(...precios)
}

/** Ordena las salidas (DEC-164) antes de validar y enviar, para que el índice de un error del servidor sea el de la fila. */
export function ordenarSalidas(borrador: BorradorServicio): BorradorServicio {
  const salidas = [...borrador.tour.salidas].sort((a, b) => {
    if (a.hora === '') return 1
    if (b.hora === '') return -1
    return a.hora.localeCompare(b.hora)
  })
  return { ...borrador, tour: { ...borrador.tour, salidas } }
}

const ordenados = <T extends string>(elegidos: readonly T[], orden: readonly T[]) => orden.filter((valor) => elegidos.includes(valor))
const texto = (valor: string) => valor.trim()
const opcional = (valor: string) => (valor.trim() === '' ? undefined : valor.trim())

function detalleCategoria(borrador: BorradorServicio, categoria: Categoria): Record<string, unknown> {
  switch (categoria) {
    case 'Alojamiento':
      return {
        serviciosIncluidos: ordenados(borrador.alojamiento.serviciosIncluidos, SERVICIOS_INCLUIDOS),
        unidades: borrador.alojamiento.unidades.map((unidad) => ({
          nombre: texto(unidad.nombre),
          tipoHabitacion: texto(unidad.tipoHabitacion),
          numHabitaciones: aNumero(unidad.numHabitaciones),
          capacidadMaxHuespedes: aNumero(unidad.capacidadMaxHuespedes),
          precioNoche: aNumero(unidad.precioNoche),
        })),
      }
    case 'Tour':
      return {
        puntoEncuentro: texto(borrador.tour.puntoEncuentro),
        duracionHoras: aNumero(borrador.tour.duracionHoras),
        nivelDificultad: opcional(borrador.tour.nivelDificultad),
        aforoMaxGrupo: aNumero(borrador.tour.aforoMaxGrupo),
      }
    case 'Gastronomia':
      return {
        tipoCocina: texto(borrador.gastronomia.tipoCocina),
        aforoMaxTurno: aNumero(borrador.gastronomia.aforoMaxTurno),
        menuVegetariano: borrador.gastronomia.menuVegetariano,
        rangoPrecioPlato: texto(borrador.gastronomia.rangoPrecioPlato),
      }
    case 'AlquilerEquipo':
      return {
        tipoVehiculoEquipo: texto(borrador.alquiler.tipoVehiculoEquipo),
        unidadesDisponibles: aNumero(borrador.alquiler.unidadesDisponibles),
        condicionesAlquiler: opcional(borrador.alquiler.condicionesAlquiler),
      }
  }
}

/**
 * Cuerpo de `POST /api/v1/catalogo/prestador/servicios` (DEC-163): solo la categoría elegida. Sin `precioBase` en
 * Alojamiento (lo deriva el servidor, DEC-127), sin `oferta` en Gastronomía y Alquiler y sin `salidas` en
 * Alojamiento (DEC-136). Nunca envía lo que fija el servidor (`rnt`, `estadoPublicacion`, `prestadorId`…, DEC-141).
 */
export function construirCuerpo(borrador: BorradorServicio): Record<string, unknown> {
  const categoria = borrador.categoria as Categoria
  const cuerpo: Record<string, unknown> = {
    nombreComercial: texto(borrador.nombreComercial),
    categoria,
    descripcion: texto(borrador.descripcion),
    direccion: texto(borrador.direccion),
    coordenadas: { longitud: aNumero(borrador.longitud), latitud: aNumero(borrador.latitud) },
    contacto: { telefono: texto(borrador.telefono), correo: texto(borrador.correo) },
    condicionesAccesibilidad: ordenados(borrador.condicionesAccesibilidad, CONDICIONES_ACCESIBILIDAD),
  }
  const horario = opcional(borrador.horarioAtencion)
  if (horario !== undefined) cuerpo.horarioAtencion = horario
  if (categoria !== 'Alojamiento') cuerpo.precioBase = aNumero(borrador.precioBase)
  cuerpo.detalleCategoria = detalleCategoria(borrador, categoria)
  if (categoria === 'Tour') {
    cuerpo.oferta = {
      diasOperacion: ordenados(borrador.tour.diasOperacion, DIAS_SEMANA),
      salidas: borrador.tour.salidas.map((salida) => salida.hora),
    }
  } else if (categoria === 'Alojamiento') {
    cuerpo.oferta = { diasOperacion: ordenados(borrador.alojamiento.diasOperacion, DIAS_SEMANA) }
  }
  return cuerpo
}

/**
 * Validación en el cliente (DEC-166): obligatorios, formatos básicos y longitudes de texto. Los rangos numéricos
 * superiores y las demás reglas quedan en el servidor, que es la fuente única; sus errores llegan en el 400.
 */
export function validarBorrador(borrador: BorradorServicio): ErroresServicio {
  const errores: ErroresServicio = {}
  const textoObligatorio = (ruta: string, valor: string) => {
    const longitud = longitudTexto(valor)
    if (longitud === 0) errores[ruta] = 'REQUERIDO'
    else if (longitud > LONGITUD_MAXIMA[ruta.replace(/\[\d+\]/g, '')]) errores[ruta] = 'LONGITUD_MAXIMA'
  }
  const textoOpcional = (ruta: string, valor: string) => {
    if (longitudTexto(valor) > LONGITUD_MAXIMA[ruta]) errores[ruta] = 'LONGITUD_MAXIMA'
  }
  const numero = (ruta: string, valor: string, entero: boolean) => {
    const limpio = valor.trim()
    if (limpio === '') errores[ruta] = 'REQUERIDO'
    else if (!(entero ? FORMA_ENTERO : FORMA_DECIMAL).test(limpio)) errores[ruta] = 'FORMATO'
    else if ((aNumero(limpio) ?? 0) <= 0) errores[ruta] = 'VALOR_MINIMO'
  }
  const coordenada = (ruta: string, valor: string, limite: number) => {
    const numeroLeido = aNumero(valor)
    if (valor.trim() === '') errores[ruta] = 'REQUERIDO'
    else if (numeroLeido === null) errores[ruta] = 'FORMATO'
    else if (numeroLeido < -limite) errores[ruta] = 'VALOR_MINIMO'
    else if (numeroLeido > limite) errores[ruta] = 'VALOR_MAXIMO'
  }

  textoObligatorio('nombreComercial', borrador.nombreComercial)
  if (borrador.categoria === '') errores.categoria = 'REQUERIDO'
  textoObligatorio('descripcion', borrador.descripcion)
  textoObligatorio('contacto.telefono', borrador.telefono)
  textoObligatorio('contacto.correo', borrador.correo)
  if (!errores['contacto.correo'] && !FORMA_CORREO.test(borrador.correo.trim())) errores['contacto.correo'] = 'FORMATO'
  textoObligatorio('direccion', borrador.direccion)
  coordenada('coordenadas.latitud', borrador.latitud, 90)
  coordenada('coordenadas.longitud', borrador.longitud, 180)
  textoOpcional('horarioAtencion', borrador.horarioAtencion)

  const categoria = borrador.categoria
  if (categoria !== '' && categoria !== 'Alojamiento') numero('precioBase', borrador.precioBase, false)

  if (categoria === 'Alojamiento') {
    borrador.alojamiento.unidades.forEach((unidad, indice) => {
      const ruta = (campo: string) => `detalleCategoria.unidades[${indice}].${campo}`
      textoObligatorio(ruta('nombre'), unidad.nombre)
      textoObligatorio(ruta('tipoHabitacion'), unidad.tipoHabitacion)
      numero(ruta('numHabitaciones'), unidad.numHabitaciones, true)
      numero(ruta('capacidadMaxHuespedes'), unidad.capacidadMaxHuespedes, true)
      numero(ruta('precioNoche'), unidad.precioNoche, false)
    })
    if (borrador.alojamiento.diasOperacion.length === 0) errores['oferta.diasOperacion'] = 'REQUERIDO'
  } else if (categoria === 'Tour') {
    textoObligatorio('detalleCategoria.puntoEncuentro', borrador.tour.puntoEncuentro)
    numero('detalleCategoria.duracionHoras', borrador.tour.duracionHoras, false)
    textoOpcional('detalleCategoria.nivelDificultad', borrador.tour.nivelDificultad)
    numero('detalleCategoria.aforoMaxGrupo', borrador.tour.aforoMaxGrupo, true)
    if (borrador.tour.diasOperacion.length === 0) errores['oferta.diasOperacion'] = 'REQUERIDO'
    const vistas = new Set<string>()
    borrador.tour.salidas.forEach((salida, indice) => {
      const ruta = `oferta.salidas[${indice}]`
      if (salida.hora === '') errores[ruta] = 'REQUERIDO'
      else if (!FORMA_HORA.test(salida.hora)) errores[ruta] = 'FORMATO'
      else if (vistas.has(salida.hora)) errores[ruta] = 'DUPLICADO'
      vistas.add(salida.hora)
    })
  } else if (categoria === 'Gastronomia') {
    textoObligatorio('detalleCategoria.tipoCocina', borrador.gastronomia.tipoCocina)
    numero('detalleCategoria.aforoMaxTurno', borrador.gastronomia.aforoMaxTurno, true)
    textoObligatorio('detalleCategoria.rangoPrecioPlato', borrador.gastronomia.rangoPrecioPlato)
  } else if (categoria === 'AlquilerEquipo') {
    textoObligatorio('detalleCategoria.tipoVehiculoEquipo', borrador.alquiler.tipoVehiculoEquipo)
    numero('detalleCategoria.unidadesDisponibles', borrador.alquiler.unidadesDisponibles, true)
    textoOpcional('detalleCategoria.condicionesAlquiler', borrador.alquiler.condicionesAlquiler)
  }
  return errores
}

/**
 * Rutas con un control en pantalla para la categoría elegida, en el orden del formulario: ordenan el foco del primer
 * error y deciden qué error del servidor no tiene dónde mostrarse (entonces va al mensaje general).
 */
export function rutasConControl(borrador: BorradorServicio): string[] {
  const rutas = [
    'nombreComercial',
    'categoria',
    'descripcion',
    'contacto.telefono',
    'contacto.correo',
    'direccion',
    'coordenadas',
    'coordenadas.latitud',
    'coordenadas.longitud',
    'horarioAtencion',
  ]
  const categoria = borrador.categoria
  if (categoria !== '' && categoria !== 'Alojamiento') rutas.push('precioBase')
  if (categoria === 'Alojamiento') {
    rutas.push('detalleCategoria.serviciosIncluidos', 'detalleCategoria.unidades')
    borrador.alojamiento.unidades.forEach((_unidad, indice) => {
      for (const campo of ['nombre', 'tipoHabitacion', 'numHabitaciones', 'capacidadMaxHuespedes', 'precioNoche']) {
        rutas.push(`detalleCategoria.unidades[${indice}].${campo}`)
      }
    })
    rutas.push('oferta', 'oferta.diasOperacion')
  } else if (categoria === 'Tour') {
    rutas.push(
      'detalleCategoria.puntoEncuentro',
      'detalleCategoria.duracionHoras',
      'detalleCategoria.nivelDificultad',
      'detalleCategoria.aforoMaxGrupo',
      'oferta',
      'oferta.diasOperacion',
      'oferta.salidas',
    )
    borrador.tour.salidas.forEach((_salida, indice) => rutas.push(`oferta.salidas[${indice}]`))
  } else if (categoria === 'Gastronomia') {
    rutas.push('detalleCategoria.tipoCocina', 'detalleCategoria.aforoMaxTurno', 'detalleCategoria.menuVegetariano', 'detalleCategoria.rangoPrecioPlato')
  } else if (categoria === 'AlquilerEquipo') {
    rutas.push('detalleCategoria.tipoVehiculoEquipo', 'detalleCategoria.unidadesDisponibles', 'detalleCategoria.condicionesAlquiler')
  }
  rutas.push('condicionesAccesibilidad')
  return rutas
}
