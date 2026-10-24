import { solicitar } from '../../shared/api/httpClient'

/** Contrato del perfil del turista de ms-auth (docs/sad/api/ms-auth.md, «Perfil del turista»; RF53, DEC-84). */

const RUTA = '/api/v1/auth/perfil'

export interface PerfilTurista {
  /** Solo lectura: no se edita (DEC-79). */
  correo: string
  primerNombre: string
  segundoNombre: string | null
  primerApellido: string
  segundoApellido: string | null
  /** ISO 3166-1 alfa-2. */
  paisOrigen: string
  /** De 1 a 3, en el orden del vocabulario. */
  categoriasInteres: string[]
}

/** Cuerpo del PUT: el perfil completo, sin el correo (reemplazo, no parcial). */
export type CambiosPerfil = Omit<PerfilTurista, 'correo'>

/** Solo el rol Turista: lleva el token. */
export function consultarPerfil(): Promise<PerfilTurista> {
  return solicitar<PerfilTurista>(RUTA)
}

/** Reemplaza el perfil completo y responde 200 con el resultado (DEC-84). */
export function actualizarPerfil(cambios: CambiosPerfil): Promise<PerfilTurista> {
  const { primerNombre, segundoNombre, primerApellido, segundoApellido, paisOrigen, categoriasInteres } = cambios
  // Se arma campo por campo: nunca sale `correo`, aunque llegue en el objeto.
  return solicitar<PerfilTurista>(RUTA, {
    method: 'PUT',
    cuerpo: { primerNombre, segundoNombre, primerApellido, segundoApellido, paisOrigen, categoriasInteres },
  })
}
