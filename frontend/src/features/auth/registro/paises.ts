export interface OpcionPais {
  codigo: string
  nombre: string
}

/**
 * Nombres de los países en el idioma activo con `Intl.DisplayNames`, ordenados por el nombre traducido (no por el
 * código). Si el navegador no tiene nombre para un código, se muestra el código. No hay listas de países en el frontend:
 * los códigos llegan de GET /api/v1/auth/paises (decisión D5).
 */
export function nombresPaises(codigos: readonly string[], idioma: string): OpcionPais[] {
  let nombres: Intl.DisplayNames | null = null
  try {
    nombres = new Intl.DisplayNames([idioma], { type: 'region', fallback: 'code' })
  } catch {
    nombres = null
  }
  const orden = new Intl.Collator(idioma)
  return codigos
    .map((codigo) => {
      let nombre: string | undefined
      try {
        nombre = nombres?.of(codigo)
      } catch {
        nombre = undefined
      }
      return { codigo, nombre: nombre || codigo }
    })
    .sort((a, b) => orden.compare(a.nombre, b.nombre))
}
