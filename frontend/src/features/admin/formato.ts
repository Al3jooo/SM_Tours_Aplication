/** «aaaa-mm-dd» (día, sin hora) como fecha corta del idioma activo: 18/10/2026 en español. */
export function formatearFecha(fecha: string, idioma: string): string {
  const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(fecha)
  if (!partes) return fecha
  const instante = Date.UTC(Number(partes[1]), Number(partes[2]) - 1, Number(partes[3]))
  return new Intl.DateTimeFormat(idioma, { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC' }).format(
    instante,
  )
}

/** Nombre del país (ISO 3166-1 alfa-2) en el idioma activo; si el navegador no lo tiene, el código. */
export function nombrePais(codigo: string, idioma: string): string {
  try {
    return new Intl.DisplayNames([idioma], { type: 'region', fallback: 'code' }).of(codigo) || codigo
  } catch {
    return codigo
  }
}
