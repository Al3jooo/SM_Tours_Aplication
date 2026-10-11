import {
  CATEGORIAS,
  CONDICIONES_ACCESIBILIDAD,
  ORDENES,
  type Categoria,
  type CondicionAccesibilidad,
  type CriteriosBusqueda,
  type Orden,
} from './catalogoApi'

/**
 * Los filtros, el orden y la página viven en la URL de /explorar, para que funcionen el botón Atrás, la recarga y
 * los enlaces compartidos. La ubicación nunca va en la URL. Los valores inválidos (p. ej. de un enlace manipulado)
 * se ignoran en lugar de romper la pantalla.
 */
export function leerCriterios(parametros: URLSearchParams): CriteriosBusqueda {
  const texto = parametros.get('texto')?.trim()
  const categoria = parametros.get('categoria')
  const orden = parametros.get('orden')
  const pagina = entero(parametros.get('pagina'))
  return {
    texto: texto ? texto.slice(0, 100) : undefined,
    categoria: CATEGORIAS.includes(categoria as Categoria) ? (categoria as Categoria) : undefined,
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
  if (criterios.precioMax !== undefined) parametros.set('precioMax', String(criterios.precioMax))
  if (criterios.distanciaKm !== undefined) parametros.set('distanciaKm', String(criterios.distanciaKm))
  criterios.accesibilidad.forEach((condicion) => parametros.append('accesibilidad', condicion))
  if (criterios.orden) parametros.set('orden', criterios.orden)
  if (criterios.pagina > 1) parametros.set('pagina', String(criterios.pagina))
  return parametros
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
