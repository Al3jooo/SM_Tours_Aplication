/**
 * Base común de los mapas Leaflet: el de WF-05 (`MapaOferta`) y el selector de ubicación de WF-08
 * (`SelectorUbicacion`, DEC-165).
 *
 * Política de uso de las teselas de OpenStreetMap (único recurso externo permitido, ver CLAUDE.md): atribución
 * siempre visible, zoom máximo 19, solo las teselas del área visible (sin prefetching, caché offline ni descarga
 * masiva) y el Referer por defecto del navegador.
 */
export const URL_TESELAS = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
export const ATRIBUCION_OSM = '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
export const ZOOM_MAXIMO = 19
export const ZOOM_INICIAL = 12

/** SVG fijo (sin datos de la API). El color sale de la clase del marcador, con los tokens de estilos.css. */
export const SVG_PIN =
  '<svg viewBox="0 0 28 36" width="28" height="36" aria-hidden="true" focusable="false">' +
  '<path class="marcador__pin" d="M14 1.5C7.1 1.5 1.75 6.8 1.75 13.5 1.75 22.6 14 34.5 14 34.5s12.25-11.9 12.25-21C26.25 6.8 20.9 1.5 14 1.5Z"/>' +
  '<circle class="marcador__centro" cx="14" cy="13.5" r="4.5"/></svg>'
