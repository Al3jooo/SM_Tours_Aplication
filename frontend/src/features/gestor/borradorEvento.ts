import type { TFunction } from 'i18next'
import { validarTexto } from '../admin/textoAcotado'
import { esFechaIso } from '../reservas/fechasBogota'
import type { ErroresServicio, MotivoServicio } from '../prestador/rutasCampos'
import type { DatosEvento } from './eventosGestorApi'

/**
 * Borrador de WF-25 «Registrar evento» (RF25). Las reglas son las de `EventoTuristico` (ms-catalog): el cliente avisa
 * antes de enviar y el servidor sigue siendo la fuente. «No anterior a hoy» (DEC-242) lo decide **solo** el servidor,
 * con su `RelojPort` en Bogotá: el cliente no calcula «hoy» (DEC-248).
 */
export type BorradorEvento = DatosEvento

/** Máximos de DEC-236, en puntos de código y sin los espacios de los extremos. */
export const MAXIMO_EVENTO: Record<string, number> = {
  nombre: 120,
  ubicacion: 200,
  descripcion: 1000,
}

/** Orden de los controles en la pantalla: el foco va al primero con error. */
export const RUTAS_EVENTO = ['nombre', 'fechaInicio', 'fechaFin', 'ubicacion', 'descripcion'] as const

export function borradorEventoVacio(): BorradorEvento {
  return { nombre: '', fechaInicio: '', fechaFin: '', ubicacion: '', descripcion: '' }
}

function validarFecha(fecha: string): MotivoServicio | null {
  if (fecha.trim() === '') return 'REQUERIDO'
  return esFechaIso(fecha.trim()) ? null : 'FORMATO'
}

/** Mismos códigos que `campos` del 400 DATOS_INVALIDOS. Fin anterior al inicio es `NO_PERMITIDO` (DEC-237). */
export function validarBorradorEvento(borrador: BorradorEvento): ErroresServicio {
  const errores: ErroresServicio = {}
  for (const ruta of ['nombre', 'ubicacion', 'descripcion'] as const) {
    const motivo = validarTexto(borrador[ruta], MAXIMO_EVENTO[ruta])
    if (motivo) errores[ruta] = motivo
  }
  const inicio = validarFecha(borrador.fechaInicio)
  const fin = validarFecha(borrador.fechaFin)
  if (inicio) errores.fechaInicio = inicio
  if (fin) errores.fechaFin = fin
  // Dos `yyyy-MM-dd` válidos se comparan como texto: sin zona horaria.
  if (!inicio && !fin && borrador.fechaFin.trim() < borrador.fechaInicio.trim()) errores.fechaFin = 'NO_PERMITIDO'
  return errores
}

/** El cuerpo del POST: los textos sin los espacios de los extremos, como los cuenta el contador y los guarda ms-catalog. */
export function cuerpoEvento(borrador: BorradorEvento): DatosEvento {
  return {
    nombre: borrador.nombre.trim(),
    fechaInicio: borrador.fechaInicio.trim(),
    fechaFin: borrador.fechaFin.trim(),
    ubicacion: borrador.ubicacion.trim(),
    descripcion: borrador.descripcion.trim(),
  }
}

/**
 * Texto del error de un campo. `fechaFin = NO_PERMITIDO` tiene dos causas con el mismo código (DEC-237 y DEC-242): si
 * en el borrador el fin no es anterior al inicio, el rechazo solo puede ser «ya terminó» (decidido por el servidor).
 */
export function textoErrorEvento(
  t: TFunction,
  ruta: string,
  motivo: MotivoServicio | undefined,
  borrador: BorradorEvento,
): string | undefined {
  if (!motivo) return undefined
  if (ruta === 'fechaFin' && motivo === 'NO_PERMITIDO') {
    const ordenValido =
      esFechaIso(borrador.fechaInicio) && esFechaIso(borrador.fechaFin) && borrador.fechaFin >= borrador.fechaInicio
    return t(ordenValido ? 'gestor.eventos.errores.finPasado' : 'gestor.eventos.errores.finAntesDeInicio')
  }
  return t(`prestador.errores.${motivo}`, { maximo: MAXIMO_EVENTO[ruta] ?? '' })
}
