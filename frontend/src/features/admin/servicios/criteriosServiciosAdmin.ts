import { ESTADOS_PUBLICACION, type EstadoPublicacion } from '../../servicios/servicioGestion'

/**
 * Criterios de la cola de WF-19 en la URL (DEC-175): `estado` (por defecto `Pendiente`, como la API; sin «Todos») y
 * `pagina` (desde 1; la API empieza en 0). Un valor que no es válido se lee como el de por defecto, que no se escribe.
 */
export interface CriteriosServiciosAdmin {
  estado: EstadoPublicacion
  pagina: number
}

const ESTADO_POR_DEFECTO: EstadoPublicacion = 'Pendiente'

const esEstado = (valor: string | null): valor is EstadoPublicacion =>
  (ESTADOS_PUBLICACION as readonly (string | null)[]).includes(valor)

export function leerCriteriosServiciosAdmin(parametros: URLSearchParams): CriteriosServiciosAdmin {
  const estado = parametros.get('estado')
  const pagina = Number(parametros.get('pagina'))
  return {
    estado: esEstado(estado) ? estado : ESTADO_POR_DEFECTO,
    pagina: Number.isInteger(pagina) && pagina >= 1 ? pagina : 1,
  }
}

export function escribirCriteriosServiciosAdmin(criterios: CriteriosServiciosAdmin): URLSearchParams {
  const parametros = new URLSearchParams()
  if (esEstado(criterios.estado) && criterios.estado !== ESTADO_POR_DEFECTO) parametros.set('estado', criterios.estado)
  if (criterios.pagina > 1) parametros.set('pagina', String(criterios.pagina))
  return parametros
}
