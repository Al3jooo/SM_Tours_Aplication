import { createContext } from 'react'
import type { UsuarioActual } from './authApi'

export type EstadoSesion =
  | { estado: 'cargando' }
  | { estado: 'anonimo' }
  | { estado: 'autenticado'; usuario: UsuarioActual }

/** Por qué terminó una sesión guardada: 401 (expirada) o 403 CUENTA_INACTIVA (RF22, RF62). */
export type AvisoSesion = 'expirada' | 'inactiva'

export interface ValorSesion {
  sesion: EstadoSesion
  /** Aviso no intrusivo para las páginas públicas; en una ruta privada, RutaProtegida lleva al login. */
  aviso: AvisoSesion | null
  descartarAviso: () => void
  iniciarSesion: (correo: string, contrasena: string) => Promise<void>
  /** RF55. Quien la llama decide a dónde navegar. */
  cerrarSesion: () => void
}

export const SesionContext = createContext<ValorSesion | null>(null)
