import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError, alExpirarSesion } from '../../shared/api/httpClient'
import { borrarToken, guardarToken, leerToken } from '../../shared/api/tokenSesion'
import { consultarUsuarioActual, iniciarSesion as solicitarInicioSesion } from './authApi'
import { SesionContext, type AvisoSesion, type EstadoSesion, type ValorSesion } from './SesionContext'

/** 401 → la sesión expiró; 403 → la cuenta dejó de estar activa. Otros errores no cierran la sesión. */
function avisoPara(error: unknown): AvisoSesion | null {
  if (!(error instanceof ApiError)) return null
  if (error.status === 401) return 'expirada'
  if (error.status === 403) return 'inactiva'
  return null
}

export function SesionProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<EstadoSesion>(() =>
    leerToken() ? { estado: 'cargando' } : { estado: 'anonimo' },
  )
  const [aviso, setAviso] = useState<AvisoSesion | null>(null)

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
        const motivo = avisoPara(error)
        if (motivo) {
          borrarToken()
        }
        if (vigente) {
          setSesion({ estado: 'anonimo' })
          setAviso(motivo)
        }
      })
    return () => {
      vigente = false
    }
  }, [])

  // Cualquier petición autenticada que reciba 401 (o 403 por cuenta inactiva) termina la sesión.
  useEffect(
    () =>
      alExpirarSesion((error) => {
        borrarToken()
        setSesion({ estado: 'anonimo' })
        setAviso(avisoPara(error))
      }),
    [],
  )

  const iniciarSesion = useCallback(async (correo: string, contrasena: string) => {
    const { token } = await solicitarInicioSesion(correo, contrasena)
    guardarToken(token)
    try {
      const usuario = await consultarUsuarioActual()
      setSesion({ estado: 'autenticado', usuario })
      setAviso(null)
    } catch (error) {
      borrarToken()
      throw error
    }
  }, [])

  const cerrarSesion = useCallback(() => {
    borrarToken()
    setSesion({ estado: 'anonimo' })
    setAviso(null)
  }, [])

  const descartarAviso = useCallback(() => setAviso(null), [])

  const valor = useMemo<ValorSesion>(
    () => ({ sesion, aviso, descartarAviso, iniciarSesion, cerrarSesion }),
    [sesion, aviso, descartarAviso, iniciarSesion, cerrarSesion],
  )

  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>
}
