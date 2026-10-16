import type { CupoHorario, Disponibilidad } from './reservasApi'
import { horaCorta } from './fechasBogota'

/**
 * Cupo de una unidad para TODAS las noches de la estadía: el mínimo del rango (una reserva descuenta cada noche).
 * Una noche sin inventario publicado o con 0 cupos deja la unidad sin cupo y se informa cuál es.
 */
export function cupoUnidad(
  disponibilidad: Disponibilidad,
  idUnidad: string,
  noches: string[],
): { cupo: number; nocheSinCupo: string | null } {
  if (noches.length === 0) return { cupo: 0, nocheSinCupo: null }
  const filas = disponibilidad.unidades.find((unidad) => unidad.idUnidad === idUnidad)?.noches ?? []
  let cupo = Number.POSITIVE_INFINITY
  for (const noche of noches) {
    const disponibles = filas.find((fila) => fila.fecha === noche)?.cuposDisponibles ?? 0
    if (disponibles <= 0) return { cupo: 0, nocheSinCupo: noche }
    cupo = Math.min(cupo, disponibles)
  }
  return { cupo, nocheSinCupo: null }
}

/** Horarios publicados de un día, en orden. */
export function horariosDelDia(disponibilidad: Disponibilidad, fecha: string): CupoHorario[] {
  return disponibilidad.horarios
    .filter((horario) => horario.fecha === fecha)
    .sort((a, b) => a.hora.localeCompare(b.hora))
}

export function buscarHorario(disponibilidad: Disponibilidad, fecha: string, hora: string): CupoHorario | undefined {
  return horariosDelDia(disponibilidad, fecha).find((horario) => horaCorta(horario.hora) === hora)
}
