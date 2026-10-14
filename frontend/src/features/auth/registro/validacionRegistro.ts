import type { DatosRegistroPrestador, DatosRegistroTurista } from '../authApi'

/**
 * Validación del registro en el cliente con los mismos criterios que ms-auth (RegistroTurista, RegistroPrestador y sus
 * objetos de valor) y los mismos códigos de `campos` de DATOS_INVALIDOS. El backend sigue validando: esto solo evita un
 * viaje al servidor y muestra los errores antes.
 */

/** Motivos de `campos` (MotivoInvalido en ms-auth) y los dos errores de negocio que se muestran en un campo. */
export type MotivoCampo =
  | 'REQUERIDO'
  | 'FORMATO'
  | 'LONGITUD_MINIMA'
  | 'LONGITUD_MAXIMA'
  | 'NO_PERMITIDO'
  | 'DUPLICADO'
  | 'POLITICAS_NO_ACEPTADAS'
  | 'RNT_YA_REGISTRADO'

export type ErroresCampos = Partial<Record<string, MotivoCampo>>

/** Decisión D1: vocabulario cerrado (CHECK en V4). Las etiquetas se traducen con i18n. */
export const CATEGORIAS_INTERES = ['Naturaleza', 'Cultura', 'Aventura'] as const

/** Longitudes de las columnas de V1 (las mismas constantes del dominio). */
export const LONGITUD_MAXIMA: Record<string, number> = {
  primerNombre: 50,
  segundoNombre: 50,
  primerApellido: 50,
  segundoApellido: 50,
  correo: 100,
  correoCorporativo: 100,
  nombreRepresentanteLegal: 150,
  razonSocial: 150,
  rnt: 50,
  telefono: 20,
}

export const LONGITUD_MINIMA_CONTRASENA = 8
/** Límite de BCrypt (RNF05), en bytes UTF-8. */
export const BYTES_MAXIMOS_CONTRASENA = 72

const FORMA_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const DOS_LETRAS = /^[A-Z]{2}$/

/** La misma normalización que el login y el registro en ms-auth: sin espacios alrededor y en minúsculas. */
export function normalizarCorreo(correo: string): string {
  return correo.trim().toLowerCase()
}

/** Caracteres (puntos de código), como `codePointCount` en Java: un emoji cuenta como uno. */
function largo(texto: string): number {
  return [...texto].length
}

class Validador {
  readonly errores: ErroresCampos = {}

  requerido(campo: string, valor: string) {
    const limpio = valor.trim()
    if (!limpio) this.anotar(campo, 'REQUERIDO')
    else this.maximo(campo, limpio)
  }

  opcional(campo: string, valor: string) {
    const limpio = valor.trim()
    if (limpio) this.maximo(campo, limpio)
  }

  correo(campo: string, valor: string) {
    const normalizado = normalizarCorreo(valor)
    if (!normalizado) this.anotar(campo, 'REQUERIDO')
    else if (largo(normalizado) > LONGITUD_MAXIMA[campo]) this.anotar(campo, 'LONGITUD_MAXIMA')
    else if (!FORMA_CORREO.test(normalizado)) this.anotar(campo, 'FORMATO')
  }

  contrasena(valor: string) {
    if (!valor) this.anotar('contrasena', 'REQUERIDO')
    else if (largo(valor) < LONGITUD_MINIMA_CONTRASENA) this.anotar('contrasena', 'LONGITUD_MINIMA')
    else if (new TextEncoder().encode(valor).length > BYTES_MAXIMOS_CONTRASENA) {
      this.anotar('contrasena', 'LONGITUD_MAXIMA')
    }
  }

  politicas(acepto: boolean) {
    if (!acepto) this.anotar('aceptoPoliticas', 'POLITICAS_NO_ACEPTADAS')
  }

  private maximo(campo: string, valor: string) {
    if (largo(valor) > LONGITUD_MAXIMA[campo]) this.anotar(campo, 'LONGITUD_MAXIMA')
  }

  private anotar(campo: string, motivo: MotivoCampo) {
    this.errores[campo] ??= motivo
  }
}

/**
 * @param paises códigos admitidos (de GET /api/v1/auth/paises); null si aún no cargaron, y entonces solo se comprueba
 *               que sean dos letras
 */
export function validarTurista(datos: DatosRegistroTurista, paises: readonly string[] | null): ErroresCampos {
  const v = new Validador()
  v.requerido('primerNombre', datos.primerNombre)
  v.opcional('segundoNombre', datos.segundoNombre)
  v.requerido('primerApellido', datos.primerApellido)
  v.opcional('segundoApellido', datos.segundoApellido)
  v.correo('correo', datos.correo)
  v.contrasena(datos.contrasena)
  const pais = datos.paisOrigen.trim().toUpperCase()
  if (!pais) v.errores.paisOrigen = 'REQUERIDO'
  else if (!DOS_LETRAS.test(pais)) v.errores.paisOrigen = 'FORMATO'
  else if (paises && !paises.includes(pais)) v.errores.paisOrigen = 'NO_PERMITIDO'
  const categorias = datos.categoriasInteres
  if (categorias.length === 0) v.errores.categoriasInteres = 'REQUERIDO'
  else if (categorias.some((c) => !(CATEGORIAS_INTERES as readonly string[]).includes(c))) {
    v.errores.categoriasInteres = 'NO_PERMITIDO'
  } else if (new Set(categorias).size !== categorias.length) v.errores.categoriasInteres = 'DUPLICADO'
  v.politicas(datos.aceptoPoliticas)
  return v.errores
}

export function validarPrestador(datos: DatosRegistroPrestador): ErroresCampos {
  const v = new Validador()
  v.requerido('nombreRepresentanteLegal', datos.nombreRepresentanteLegal)
  v.opcional('razonSocial', datos.razonSocial ?? '')
  v.requerido('rnt', datos.rnt)
  v.correo('correoCorporativo', datos.correoCorporativo)
  v.requerido('telefono', datos.telefono)
  v.contrasena(datos.contrasena)
  v.politicas(datos.aceptoPoliticas)
  return v.errores
}

const MOTIVOS = new Set<string>([
  'REQUERIDO',
  'FORMATO',
  'LONGITUD_MINIMA',
  'LONGITUD_MAXIMA',
  'NO_PERMITIDO',
  'DUPLICADO',
])

/** `campos` de un ProblemDetail DATOS_INVALIDOS: solo los códigos conocidos (nunca se muestra texto del servidor). */
export function erroresDelServidor(campos: unknown): ErroresCampos {
  if (!campos || typeof campos !== 'object') return {}
  const errores: ErroresCampos = {}
  for (const [campo, motivo] of Object.entries(campos as Record<string, unknown>)) {
    if (typeof motivo === 'string' && MOTIVOS.has(motivo)) errores[campo] = motivo as MotivoCampo
  }
  return errores
}
