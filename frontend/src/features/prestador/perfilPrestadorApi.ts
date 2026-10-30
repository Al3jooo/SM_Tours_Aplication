import { solicitar } from '../../shared/api/httpClient'

/**
 * Perfil comercial del Prestador de ms-auth (docs/sad/api/ms-auth.md, «Perfil comercial del Prestador»; RF43,
 * DEC-188). Solo el rol Prestador; con token.
 */

const RUTA = '/api/v1/auth/prestador/perfil'

export interface PerfilPrestador {
  /** La credencial: solo lectura, no se edita (ADR-006, decisión D2). */
  correo: string
  representanteLegal: string
  /** Puede ser null. */
  razonSocial: string | null
  rnt: string
  /** El correo de contacto de la empresa, no la credencial. */
  correoCorporativo: string
  telefono: string
}

/** Lo único que cambia el PUT. */
export type CambiosPerfilPrestador = Pick<PerfilPrestador, 'correoCorporativo' | 'telefono'>

export function consultarPerfilPrestador(): Promise<PerfilPrestador> {
  return solicitar<PerfilPrestador>(RUTA)
}

/** 200 con el perfil resultante. Se arma campo por campo: nunca salen el correo de acceso, el RNT ni los demás. */
export function actualizarPerfilPrestador(cambios: CambiosPerfilPrestador): Promise<PerfilPrestador> {
  const { correoCorporativo, telefono } = cambios
  return solicitar<PerfilPrestador>(RUTA, { method: 'PUT', cuerpo: { correoCorporativo, telefono } })
}
