import {
  CATEGORIAS,
  CONDICIONES_ACCESIBILIDAD,
  ORDENES,
  CAMPOS_POR_CATEGORIA,
  SERVICIOS_INCLUIDOS,
  agregarMasFiltros,
  soloDeCategoria,
  type Categoria,
  type CondicionAccesibilidad,
  type CriteriosBusqueda,
  type MasFiltros,
  type Orden,
  type ServicioIncluido,
} from './catalogoApi'

/** Como la API: un filtro de texto admite como máximo 100 caracteres. */
const LONGITUD_MAXIMA_TEXTO = 100

/**
 * Los filtros, el orden y la página viven en la URL de /explorar, para que funcionen el botón Atrás, la recarga y
 * los enlaces compartidos. La ubicación nunca va en la URL. Los valores inválidos (p. ej. de un enlace manipulado)
 * se ignoran en lugar de romper la pantalla. Igual los de «Más filtros» que no son de la categoría de la URL (o si no
 * hay categoría): se descartan en silencio, porque la API los rechazaría.
 */
export function leerCriterios(parametros: URLSearchParams): CriteriosBusqueda {
  const texto = textoAcotado(parametros.get('texto'))
  const categoriaUrl = parametros.get('categoria')
  const categoria = CATEGORIAS.includes(categoriaUrl as Categoria) ? (categoriaUrl as Categoria) : undefined
  const orden = parametros.get('orden')
  const pagina = entero(parametros.get('pagina'))
  return {
    texto,
    categoria,
    masFiltros: soloDeCategoria(leerMasFiltros(parametros), categoria),
    precioMax: numeroNoNegativo(parametros.get('precioMax')),
    distanciaKm: numeroPositivo(parametros.get('distanciaKm')),
    accesibilidad: [...new Set(parametros.getAll('accesibilidad'))].filter((c): c is CondicionAccesibilidad =>
      CONDICIONES_ACCESIBILIDAD.includes(c as CondicionAccesibilidad),
    ),
    orden: ORDENES.includes(orden as Orden) ? (orden as Orden) : undefined,
    pagina: pagina !== undefined && pagina >= 1 ? pagina : 1,
  }
}

/** Orden estable de los parámetros; la página 1 se omite. */
export function escribirCriterios(criterios: CriteriosBusqueda): URLSearchParams {
  const parametros = new URLSearchParams()
  if (criterios.texto) parametros.set('texto', criterios.texto)
  if (criterios.categoria) parametros.set('categoria', criterios.categoria)
  agregarMasFiltros(parametros, soloDeCategoria(criterios.masFiltros, criterios.categoria))
  if (criterios.precioMax !== undefined) parametros.set('precioMax', String(criterios.precioMax))
  if (criterios.distanciaKm !== undefined) parametros.set('distanciaKm', String(criterios.distanciaKm))
  criterios.accesibilidad.forEach((condicion) => parametros.append('accesibilidad', condicion))
  if (criterios.orden) parametros.set('orden', criterios.orden)
  if (criterios.pagina > 1) parametros.set('pagina', String(criterios.pagina))
  return parametros
}

const CLAVES_MAS_FILTROS = new Set<string>(Object.values(CAMPOS_POR_CATEGORIA).flat())

/**
 * Si la URL trae parámetros de «Más filtros» que `leerCriterios` descarta (de otra categoría, sin categoría o con un
 * valor inválido), devuelve la consulta sin ellos, para reemplazar la URL sin mostrar un error; si no, null. Los
 * demás parámetros quedan como estaban.
 */
export function sinMasFiltrosDescartados(parametros: URLSearchParams): URLSearchParams | null {
  const validos = new URLSearchParams()
  agregarMasFiltros(validos, leerCriterios(parametros).masFiltros ?? {})
  const recibidos = [...parametros].filter(([clave]) => CLAVES_MAS_FILTROS.has(clave))
  const igual = (a: [string, string][], b: [string, string][]) =>
    a.length === b.length && a.every(([clave, valor]) => b.some(([c, v]) => c === clave && v === valor))
  if (igual(recibidos, [...validos])) return null
  const limpios = new URLSearchParams([...parametros].filter(([clave]) => !CLAVES_MAS_FILTROS.has(clave)))
  validos.forEach((valor, clave) => limpios.append(clave, valor))
  return limpios
}

function leerMasFiltros(parametros: URLSearchParams): MasFiltros {
  const huespedes = entero(parametros.get('huespedes'))
  return {
    serviciosIncluidos: SERVICIOS_INCLUIDOS.filter((servicio: ServicioIncluido) =>
      parametros.getAll('serviciosIncluidos').includes(servicio),
    ),
    huespedes: huespedes !== undefined && huespedes >= 1 ? huespedes : undefined,
    tipoHabitacion: textoAcotado(parametros.get('tipoHabitacion')),
    menuVegetariano: parametros.get('menuVegetariano') === 'true' || undefined,
    tipoCocina: textoAcotado(parametros.get('tipoCocina')),
    nivelDificultad: textoAcotado(parametros.get('nivelDificultad')),
    duracionMaxHoras: numeroPositivo(parametros.get('duracionMaxHoras')),
    tipoVehiculoEquipo: textoAcotado(parametros.get('tipoVehiculoEquipo')),
  }
}

function textoAcotado(valor: string | null): string | undefined {
  const limpio = valor?.trim()
  return limpio ? limpio.slice(0, LONGITUD_MAXIMA_TEXTO) : undefined
}

function entero(valor: string | null): number | undefined {
  if (valor === null || !/^\d+$/.test(valor)) return undefined
  return Number(valor)
}

function numeroNoNegativo(valor: string | null): number | undefined {
  if (valor === null || valor.trim() === '') return undefined
  const numero = Number(valor)
  return Number.isFinite(numero) && numero >= 0 ? numero : undefined
}

function numeroPositivo(valor: string | null): number | undefined {
  const numero = numeroNoNegativo(valor)
  return numero !== undefined && numero > 0 ? numero : undefined
}
