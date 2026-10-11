import type { TFunction } from 'i18next'

/** Categoría traducida; el texto libre de un atractivo que no esté traducido se muestra tal cual. */
export function etiquetaCategoria(t: TFunction, categoria: string | null): string {
  if (!categoria) return ''
  return t(`catalogo.categorias.${categoria}`, { defaultValue: categoria })
}

const IMAGEN_POR_CATEGORIA: Record<string, string> = {
  Alojamiento: 'alojamiento',
  Gastronomia: 'gastronomia',
  Tour: 'tour',
  AlquilerEquipo: 'alquiler-equipo',
  Naturaleza: 'naturaleza',
  Cultura: 'cultura',
}

/** Ilustración propia de la categoría (frontend/public/imagenes/catalogo/categorias/). */
export function imagenDeCategoria(categoria: string | null, tipo: 'Servicio' | 'Atractivo'): string {
  const archivo = (categoria && IMAGEN_POR_CATEGORIA[categoria]) ?? (tipo === 'Atractivo' ? 'atractivo' : 'tour')
  return `/imagenes/catalogo/categorias/${archivo}.svg`
}
