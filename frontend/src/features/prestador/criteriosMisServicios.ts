import { ESTADOS_PUBLICACION } from './serviciosPrestadorApi'

/**
 * Criterios de WF-18 en la URL (DEC-169): `estado` (sin él, «Todos») y `pagina` (desde 1, como «Mis reservas»; la API
 * empieza en 0). Un valor que no es válido se lee como el de por defecto, que no se escribe.
 */
export interface CriteriosMisServicios {
  estado: string
  pagina: number
}

export function leerCriteriosMisServicios(parametros: URLSearchParams): CriteriosMisServicios {
  const estado = parametros.get('estado') ?? ''
  const pagina = Number(parametros.get('pagina'))
  return {
    estado: (ESTADOS_PUBLICACION as readonly string[]).includes(estado) ? estado : '',
    pagina: Number.isInteger(pagina) && pagina >= 1 ? pagina : 1,
  }
}

export function escribirCriteriosMisServicios(criterios: CriteriosMisServicios): URLSearchParams {
  const parametros = new URLSearchParams()
  if ((ESTADOS_PUBLICACION as readonly string[]).includes(criterios.estado)) parametros.set('estado', criterios.estado)
  if (criterios.pagina > 1) parametros.set('pagina', String(criterios.pagina))
  return parametros
}
