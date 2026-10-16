import { esRutaInterna } from '../../auth/destinoTrasLogin'

/**
 * Navegación entre la búsqueda, el detalle (WF-06) y la reserva (WF-10). «Ver detalle» lleva en state.volverA la
 * búsqueda de origen (ruta y consulta) para que «Volver a la búsqueda» conserve los filtros. Solo se acepta una ruta
 * interna (sin redirección abierta).
 */
export interface EstadoVolver {
  volverA?: string
}

export function rutaDetalle(idServicio: string): string {
  return `/servicios/${encodeURIComponent(idServicio)}`
}

export function rutaReserva(idServicio: string): string {
  return `${rutaDetalle(idServicio)}/reservar`
}

/** La búsqueda de origen, si llegó una ruta interna en el estado de la navegación. */
export function leerVolverA(estado: unknown): string | null {
  const volverA = (estado as EstadoVolver | null)?.volverA
  return esRutaInterna(volverA) ? volverA : null
}
