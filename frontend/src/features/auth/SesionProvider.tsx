import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError, alExpirarSesion } from '../../shared/api/httpClient'
import { borrarToken, guardarToken, leerToken } from '../../shared/api/tokenSesion'
import { consultarUsuarioActual, iniciarSesion as solicitarInicioSesion } from './authApi'
import { LIMITE_VALIDACION_MS, SesionContext, type AvisoSesion, type EstadoSesion, type ValorSesion } from './SesionContext'

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
  // Si /me no confirma la sesión (503, error de red o más de LIMITE_VALIDACION_MS sin respuesta), la sesión queda
  // como no confirmada: estado anónimo, pero el token se conserva, porque nada demostró que sea inválido; al recargar
  // se valida de nuevo. Una respuesta tardía solo vale si el token guardado sigue siendo el mismo con el que se pidió:
  // si hubo un nuevo inicio de sesión o un cierre de sesión mientras tanto, se descarta.
  useEffect(() => {
    const token = leerToken()
    if (!token) {
      return
    }
    let vigente = true
    const mismoToken = () => vigente && leerToken() === token
    const limite = setTimeout(() => {
      if (vigente) setSesion((actual) => (actual.estado === 'cargando' ? { estado: 'anonimo' } : actual))
    }, LIMITE_VALIDACION_MS)
    consultarUsuarioActual()
      .then((usuario) => {
        if (mismoToken()) setSesion({ estado: 'autenticado', usuario })
      })
      .catch((error: unknown) => {
        // Un 401 o 403 ya lo atendió alExpirarSesion (borra el token y avisa); aquí solo queda lo que no lo es.
        if (!mismoToken()) return
        const motivo = avisoPara(error)
        if (motivo) {
          borrarToken()
        }
        setSesion({ estado: 'anonimo' })
        setAviso(motivo)
      })
      .finally(() => clearTimeout(limite))
    return () => {
      vigente = false
      clearTimeout(limite)
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
