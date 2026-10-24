import { createContext } from 'react'
import type { UsuarioActual } from './authApi'

export type EstadoSesion =
  | { estado: 'cargando' }
  | { estado: 'anonimo' }
  | { estado: 'autenticado'; usuario: UsuarioActual }

/**
 * Por qué terminó una sesión: 401 o vencimiento por `expiraEn` (expirada), 403 CUENTA_INACTIVA (inactiva; RF22, RF62)
 * o 401 SESION_INVALIDADA (invalidada: la cuenta cambió de estado o de rol después de iniciar sesión; DEC-29, DEC-49).
 */
export type AvisoSesion = 'expirada' | 'inactiva' | 'invalidada'

/** Claves i18n del texto de cada aviso y de su enlace al login. */
export const TEXTO_AVISO: Record<AvisoSesion, { aviso: string; enlace: string }> = {
  expirada: { aviso: 'sesion.avisoExpirada', enlace: 'sesion.iniciarDeNuevo' },
  invalidada: { aviso: 'sesion.avisoInvalidada', enlace: 'sesion.iniciarDeNuevo' },
  inactiva: { aviso: 'sesion.avisoInactiva', enlace: 'sesion.iniciarSesion' },
}

export interface ValorSesion {
  sesion: EstadoSesion
  /** Aviso no intrusivo para las páginas públicas; en una ruta privada, RutaProtegida lleva al login. */
  aviso: AvisoSesion | null
  descartarAviso: () => void
  iniciarSesion: (correo: string, contrasena: string) => Promise<void>
  /** RF55. Quien la llama decide a dónde navegar. */
  cerrarSesion: () => void
  /**
   * Vuelve a pedir /me para actualizar los datos del encabezado (p. ej. el nombre después de editar el perfil, DEC-97).
   * Nunca falla: con un error de red o 5xx se conservan la sesión y los datos actuales; un 401 la cierra (DEC-50).
   */
  refrescarUsuario: () => Promise<void>
}

/** Tiempo máximo para validar con /me una sesión guardada: el enlace «Inicio de sesión» nunca queda oculto indefinidamente. */
export const LIMITE_VALIDACION_MS = 8_000

export const SesionContext = createContext<ValorSesion | null>(null)
