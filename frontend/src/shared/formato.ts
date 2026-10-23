/** Zona de Santa Marta: las fechas que muestra la plataforma son las del municipio, no las del navegador. */
const ZONA_SANTA_MARTA = 'America/Bogota'

/**
 * Día de un instante ISO 8601 (p. ej. `fechaPublicacion` o `fechaActualizacion` de ms-catalog) como fecha corta del
 * idioma activo, en la hora de Santa Marta: «2026-10-22T03:00:00Z» es 21/10/2026. Un texto que no es una fecha se
 * devuelve tal cual. Lo usan el banner de avisos (WF-14) y las pantallas del Administrador (WF-13, WF-14).
 */
export function formatearDia(instante: string, idioma: string): string {
  const fecha = new Date(instante)
  if (Number.isNaN(fecha.getTime())) return instante
  return new Intl.DateTimeFormat(idioma, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    timeZone: ZONA_SANTA_MARTA,
  }).format(fecha)
}
