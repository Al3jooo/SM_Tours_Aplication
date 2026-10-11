import { describe, expect, it } from 'vitest'
import estilos from './estilos.css?raw'

/**
 * Guardia de contraste WCAG 2.x AA sobre los tokens reales de estilos.css. Si una pantalla futura cambia
 * un color y rompe un par, esta prueba falla. Agrega aquí los pares nuevos que use cada pantalla.
 */

const TEXTO = 4.5 // texto normal (AA)
const NO_TEXTUAL = 3 // bordes de campos, anillos de foco, iconos con significado (AA 1.4.11)

function tokens(css: string): Map<string, string> {
  const raiz = css.slice(css.indexOf(':root'), css.indexOf('}', css.indexOf(':root')))
  const mapa = new Map<string, string>()
  for (const [, nombre, valor] of raiz.matchAll(/--color-([a-z0-9-]+):\s*(#[0-9a-fA-F]{6})/g)) {
    mapa.set(nombre, valor)
  }
  return mapa
}

function luminancia(hex: string): number {
  const [r, g, b] = [1, 3, 5]
    .map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
    .map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4))
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

function contraste(a: string, b: string): number {
  const [claro, oscuro] = [luminancia(a), luminancia(b)].sort((x, y) => y - x)
  return (claro + 0.05) / (oscuro + 0.05)
}

const colores = tokens(estilos)

const PARES: [frente: string, fondo: string, minimo: number, uso: string][] = [
  ['tinta', 'arena', TEXTO, 'texto principal sobre la página'],
  ['tinta', 'superficie', TEXTO, 'texto en tarjetas'],
  ['tinta', 'arena-200', TEXTO, 'aviso de intentos'],
  ['tinta', 'sol-100', TEXTO, 'texto seleccionado'],
  ['tinta-suave', 'arena', TEXTO, 'subtítulos'],
  ['tinta-suave', 'superficie', TEXTO, 'ayudas en tarjetas, correo del menú'],
  ['tinta-suave', 'mar-100', TEXTO, 'texto del inicio provisional'],
  ['tinta-suave', 'sol-100', TEXTO, 'texto del inicio provisional (zona del sol)'],
  ['deshabilitado', 'superficie', TEXTO, '«Próximamente» en tarjetas, placeholder'],
  ['deshabilitado', 'arena', TEXTO, '«Próximamente» en la página'],
  ['superficie', 'atardecer-600', TEXTO, 'botón principal'],
  ['superficie', 'atardecer-700', TEXTO, 'botón principal en hover'],
  ['superficie', 'mar-700', TEXTO, 'idioma activo, saltar al contenido'],
  ['superficie', 'mar-900', TEXTO, 'iniciales del avatar'],
  ['mar-700', 'superficie', TEXTO, 'idioma inactivo, pestaña activa'],
  ['mar-700', 'arena', TEXTO, 'navegación activa'],
  ['mar-700', 'mar-100', TEXTO, 'idioma en hover'],
  ['mar-900', 'mar-100', TEXTO, 'saludo del inicio, opción del menú en hover'],
  ['mar-900', 'sol-100', TEXTO, 'saludo del inicio (zona del sol)'],
  ['mar-900', 'arena', TEXTO, 'título del login'],
  ['error', 'superficie', TEXTO, 'error de campo'],
  ['error', 'error-fondo', TEXTO, 'mensaje de error'],
  ['cafe-700', 'sol-100', TEXTO, 'insignia «Próximamente»'],
  ['pie-texto', 'mar-900', TEXTO, 'títulos del pie'],
  ['pie-suave', 'mar-900', TEXTO, 'enlaces y derechos del pie'],
  ['borde-campo', 'superficie', NO_TEXTUAL, 'borde de los campos'],
  ['borde-campo', 'arena', NO_TEXTUAL, 'borde de los campos sobre la página'],
  ['mar-700', 'superficie', NO_TEXTUAL, 'anillo de foco en tarjetas'],
  ['mar-700', 'arena', NO_TEXTUAL, 'anillo de foco en la página'],
  ['sol-400', 'mar-900', NO_TEXTUAL, 'anillo de foco en el pie'],
  ['atardecer-600', 'arena-200', NO_TEXTUAL, 'icono y borde del aviso de intentos'],
  ['error', 'superficie', NO_TEXTUAL, 'borde de campo inválido'],
]

describe('Tokens de color (WCAG AA)', () => {
  it.each(PARES)('%s sobre %s ≥ %s:1 (%s)', (frente, fondo, minimo) => {
    const a = colores.get(frente)
    const b = colores.get(fondo)
    expect(a, `falta el token --color-${frente}`).toBeDefined()
    expect(b, `falta el token --color-${fondo}`).toBeDefined()
    expect(contraste(a!, b!)).toBeGreaterThanOrEqual(minimo)
  })
})
