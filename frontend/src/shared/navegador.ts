/**
 * Acciones del navegador detrás de un objeto, para poder espiarlas en las pruebas (jsdom no permite reemplazar
 * window.location).
 */
export const navegador = {
  recargar: () => window.location.reload(),
}
