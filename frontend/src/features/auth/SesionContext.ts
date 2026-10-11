import { createContext } from 'react'
import type { UsuarioActual } from './authApi'

export type EstadoSesion =
  | { estado: 'cargando' }
  | { estado: 'anonimo' }
  | { estado: 'autenticado'; usuario: UsuarioActual }

export interface ValorSesion {
  sesion: EstadoSesion
  iniciarSesion: (correo: string, contrasena: string) => Promise<void>
  /** RF55. Quien la llama decide a dónde navegar. */
  cerrarSesion: () => void
}

export const SesionContext = createContext<ValorSesion | null>(null)
