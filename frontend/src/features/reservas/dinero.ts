import { localeDe } from '../catalogo/formato'

/**
 * Dinero de las reservas en CENTAVOS enteros: nada pasa por coma flotante. ms-catalog envía los precios como número o
 * como texto decimal (BigDecimal); se aceptan como máximo 2 decimales, comprobados sobre el texto. El total se arma
 * multiplicando enteros y se envía como texto decimal exacto («520001.00»), que ms-booking compara por valor (RF37).
 */
const DECIMAL = /^(\d+)(?:\.(\d{1,2}))?$/

/** Centavos de un precio, o null si no es un decimal positivo con 2 decimales como máximo (o no es un entero seguro). */
export function centavos(valor: number | string | null | undefined): number | null {
  if (valor === null || valor === undefined) return null
  // Un número se valida por su texto: 1e21 («1e+21»), -5 o 260000.123 no cumplen el formato.
  const coincidencia = DECIMAL.exec(typeof valor === 'number' ? String(valor) : valor)
  if (!coincidencia) return null
  const entero = Number(coincidencia[1])
  const total = entero * 100 + Number((coincidencia[2] ?? '').padEnd(2, '0'))
  return Number.isSafeInteger(entero) && Number.isSafeInteger(total) ? total : null
}

/** Producto de enteros con la comprobación de entero seguro; null si algún factor no es válido o se desborda. */
function multiplicar(base: number | null, ...factores: number[]): number | null {
  if (base === null) return null
  let resultado = base
  for (const factor of factores) {
    if (!Number.isSafeInteger(factor) || factor < 1) return null
    resultado *= factor
    if (!Number.isSafeInteger(resultado)) return null
  }
  return resultado
}

/** Alojamiento: precio por noche de la unidad × noches × habitaciones (como ms-booking). */
export function totalAlojamiento(precioNoche: number | null, noches: number, habitaciones: number): number | null {
  return multiplicar(precioNoche, noches, habitaciones)
}

/** Tour: precio por persona × personas. */
export function totalTour(precio: number | null, personas: number): number | null {
  return multiplicar(precio, personas)
}

/** Texto decimal exacto con 2 decimales: 52000100 → «520001.00». Solo división y resto de enteros. */
export function decimalExacto(valorCentavos: number): string {
  const resto = valorCentavos % 100
  return `${(valorCentavos - resto) / 100}.${String(resto).padStart(2, '0')}`
}

/** Pesos colombianos; los decimales solo aparecen si no son cero: «$ 260.000» o «$ 260.000,50». */
export function formatearCentavos(valorCentavos: number, idioma: string): string {
  const decimales = valorCentavos % 100 === 0 ? 0 : 2
  return new Intl.NumberFormat(localeDe(idioma), {
    style: 'currency',
    currency: 'COP',
    minimumFractionDigits: decimales,
    maximumFractionDigits: decimales,
  }).format(decimalExacto(valorCentavos) as Intl.StringNumericLiteral)
}
