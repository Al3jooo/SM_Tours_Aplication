import type { TFunction } from 'i18next'
import { soloDeCategoria } from './catalogoApi'
import { chipsMasFiltros } from './componentes/FiltrosOferta'
import { etiquetaCategoria } from './componentes/etiquetas'
import { escribirCriterios } from './criteriosUrl'
import { formatearDistancia, formatearPrecio } from './formato'
import { criteriosDesdeHistorial, type CriteriosHistorial } from './historialApi'

/**
 * Texto de una búsqueda de «Últimas búsquedas» (RF60, DEC-96), con las mismas palabras de los filtros de WF-05 y en el
 * idioma activo: «“playa” · Alojamiento · Hasta $ 200.000 · Cerca de mí · 5 km». El orden solo aparece si no es el
 * predeterminado (por distancia con ubicación, por nombre sin ella).
 */
export function textoBusqueda(criterios: CriteriosHistorial, t: TFunction, idioma: string): string {
  const partes: string[] = []
  if (criterios.texto) partes.push(t('historial.texto', { texto: criterios.texto }))
  if (criterios.categoria) partes.push(etiquetaCategoria(t, criterios.categoria))
  const pantalla = criteriosDesdeHistorial(criterios)
  partes.push(...chipsMasFiltros(soloDeCategoria(pantalla.masFiltros, pantalla.categoria), t, idioma).map((c) => c.texto))
  if (criterios.precioMax !== null) {
    partes.push(t('historial.precioMax', { precio: formatearPrecio(criterios.precioMax, idioma) }))
  }
  if (criterios.usarUbicacion) {
    partes.push(
      criterios.distanciaKm !== null
        ? t('historial.cercaDistancia', { distancia: formatearDistancia(criterios.distanciaKm, idioma) })
        : t('historial.cerca'),
    )
  }
  criterios.accesibilidad.forEach((condicion) => partes.push(t(`catalogo.accesibilidad.${condicion}`)))
  const predeterminado = criterios.usarUbicacion ? 'Distancia' : 'Nombre'
  if (criterios.orden && criterios.orden !== predeterminado) partes.push(t(`explorar.ordenes.${criterios.orden}`))
  return partes.length > 0 ? partes.join(' · ') : t('historial.sinFiltros')
}

/** Ruta de /explorar que repite la búsqueda (los criterios en la URL; la ubicación, nunca). */
export function rutaRepetir(criterios: CriteriosHistorial): string {
  const consulta = escribirCriterios(criteriosDesdeHistorial(criterios)).toString()
  return consulta ? `/explorar?${consulta}` : '/explorar'
}
