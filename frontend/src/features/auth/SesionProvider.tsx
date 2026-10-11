import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError, alExpirarSesion } from '../../shared/api/httpClient'
import { borrarToken, guardarToken, leerToken } from '../../shared/api/tokenSesion'
import { consultarUsuarioActual, iniciarSesion as solicitarInicioSesion } from './authApi'
import { SesionContext, type EstadoSesion, type ValorSesion } from './SesionContext'

export function SesionProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<EstadoSesion>(() =>
    leerToken() ? { estado: 'cargando' } : { estado: 'anonimo' },
  )

  // Al cargar la app con un token guardado, se valida con /me; un 401 o 403 cierra la sesión.
  useEffect(() => {
    if (!leerToken()) {
      return
    }
    let vigente = true
    consultarUsuarioActual()
      .then((usuario) => {
        if (vigente) setSesion({ estado: 'autenticado', usuario })
      })
      .catch((error: unknown) => {
        if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
          borrarToken()
        }
        if (vigente) setSesion({ estado: 'anonimo' })
      })
    return () => {
      vigente = false
    }
  }, [])

  // Cualquier petición autenticada que reciba 401 (o 403 por cuenta inactiva) termina la sesión.
  useEffect(
    () =>
      alExpirarSesion(() => {
        borrarToken()
        setSesion({ estado: 'anonimo' })
      }),
    [],
  )

  const iniciarSesion = useCallback(async (correo: string, contrasena: string) => {
    const { token } = await solicitarInicioSesion(correo, contrasena)
    guardarToken(token)
    try {
      const usuario = await consultarUsuarioActual()
      setSesion({ estado: 'autenticado', usuario })
    } catch (error) {
      borrarToken()
      throw error
    }
  }, [])

  const cerrarSesion = useCallback(() => {
    borrarToken()
    setSesion({ estado: 'anonimo' })
  }, [])

  const valor = useMemo<ValorSesion>(
    () => ({ sesion, iniciarSesion, cerrarSesion }),
    [sesion, iniciarSesion, cerrarSesion],
  )

  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>
}
