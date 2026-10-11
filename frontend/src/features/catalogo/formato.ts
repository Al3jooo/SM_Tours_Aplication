/**
 * Formato regional fijo por idioma: con «es» a secas el resultado dependería del navegador (p. ej. «150.000 COP»).
 */
const LOCALES: Record<string, string> = { es: 'es-CO', en: 'en-US' }

export function localeDe(idioma: string): string {
  return LOCALES[idioma.split('-')[0]] ?? LOCALES.es
}

/** Pesos colombianos sin decimales: «$ 150.000» (es-CO) o «COP 150,000» (en-US). */
export function formatearPrecio(valor: number, idioma: string): string {
  return new Intl.NumberFormat(localeDe(idioma), {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0,
  }).format(valor)
}

/** Distancia aproximada en línea recta (RF38): «0,1 km» o «0.1 km». */
export function formatearDistancia(km: number, idioma: string): string {
  return `${new Intl.NumberFormat(localeDe(idioma), { maximumFractionDigits: 1 }).format(km)} km`
}
