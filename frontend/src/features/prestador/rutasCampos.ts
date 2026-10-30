import type { TFunction } from 'i18next'

/**
 * Errores por campo del registro de servicios (DEC-166). La clave es la ruta del campo tal como la devuelve el 400
 * DATOS_INVALIDOS de ms-catalog, con punto e índice (`contacto.correo`, `detalleCategoria.unidades[0].precioNoche`,
 * `oferta.salidas[1]`), y la misma ruta da el `id` del control. Así un error del servidor y uno del cliente quedan
 * junto al mismo control.
 */
export const MOTIVOS_SERVICIO = [
  'REQUERIDO',
  'FORMATO',
  'LONGITUD_MAXIMA',
  'NO_PERMITIDO',
  'DUPLICADO',
  'VALOR_MINIMO',
  'VALOR_MAXIMO',
  'CANTIDAD_MINIMA',
  'CANTIDAD_MAXIMA',
] as const
export type MotivoServicio = (typeof MOTIVOS_SERVICIO)[number]

export type ErroresServicio = Record<string, MotivoServicio>

/** `detalleCategoria.unidades[2].precioNoche` → `servicio-detalleCategoria-unidades-2-precioNoche`. */
export function idDeCampo(ruta: string): string {
  return `servicio-${ruta.replace(/\[(\d+)\]/g, '-$1').replace(/\./g, '-')}`
}

/** La ruta sin índices: `detalleCategoria.unidades[2].precioNoche` → `detalleCategoria.unidades.precioNoche`. */
export function rutaSinIndices(ruta: string): string {
  return ruta.replace(/\[\d+\]/g, '')
}

/**
 * Longitudes máximas de los textos (DEC-138), contadas sin los espacios de los extremos y en puntos de código, como
 * ms-catalog. Solo para el contador y el aviso temprano: el servidor sigue siendo la fuente de la regla.
 */
export const LONGITUD_MAXIMA: Record<string, number> = {
  nombreComercial: 150,
  descripcion: 2000,
  direccion: 200,
  'contacto.telefono': 20,
  'contacto.correo': 254,
  horarioAtencion: 500,
  'detalleCategoria.unidades.nombre': 100,
  'detalleCategoria.unidades.tipoHabitacion': 50,
  'detalleCategoria.tipoCocina': 100,
  'detalleCategoria.rangoPrecioPlato': 50,
  'detalleCategoria.puntoEncuentro': 200,
  'detalleCategoria.nivelDificultad': 50,
  'detalleCategoria.tipoVehiculoEquipo': 100,
  'detalleCategoria.condicionesAlquiler': 500,
}

/** Solo los motivos conocidos: nunca se muestra un texto del servidor. */
export function erroresAnidados(campos: unknown): ErroresServicio {
  if (!campos || typeof campos !== 'object') return {}
  const errores: ErroresServicio = {}
  for (const [ruta, motivo] of Object.entries(campos as Record<string, unknown>)) {
    if (typeof motivo === 'string' && (MOTIVOS_SERVICIO as readonly string[]).includes(motivo)) {
      errores[ruta] = motivo as MotivoServicio
    }
  }
  return errores
}

/** Texto de `prestador.errores.<motivo>`, con `{{maximo}}` en las longitudes. */
export function textoErrorServicio(t: TFunction, ruta: string, motivo: MotivoServicio | undefined): string | undefined {
  if (!motivo) return undefined
  return t(`prestador.errores.${motivo}`, { maximo: LONGITUD_MAXIMA[rutaSinIndices(ruta)] ?? '' })
}
