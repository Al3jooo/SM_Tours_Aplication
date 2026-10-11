import { solicitar } from '../../shared/api/httpClient'

const BASE = '/api/v1/auth'

export interface SesionIniciada {
  token: string
  tipo: 'Bearer'
  expiraEn: string
  idUsuario: string
  rol: string
}

export interface UsuarioActual {
  idUsuario: string
  correo: string
  rol: string
  estadoCuenta: string
  nombreVisible: string
}

export function iniciarSesion(correo: string, contrasena: string): Promise<SesionIniciada> {
  return solicitar<SesionIniciada>(`${BASE}/login`, { method: 'POST', cuerpo: { correo, contrasena } })
}

export function consultarUsuarioActual(): Promise<UsuarioActual> {
  return solicitar<UsuarioActual>(`${BASE}/me`)
}
