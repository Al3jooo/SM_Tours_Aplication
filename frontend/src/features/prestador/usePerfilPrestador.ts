import { useCallback, useEffect, useState } from 'react'
import { consultarPerfilPrestador, type PerfilPrestador } from './perfilPrestadorApi'

export type CargaPerfilPrestador =
  | { estado: 'cargando' }
  | { estado: 'error' }
  | { estado: 'ok'; perfil: PerfilPrestador }

/**
 * `GET /api/v1/auth/prestador/perfil` (RF43): lo usan el perfil comercial (DEC-202) y el RNT de WF-08 (O16, DEC-203).
 * Un fallo no lanza: quien lo usa decide (WF-08 sigue con el texto de DEC-167). Un 401 ya cerró la sesión (httpClient).
 */
export function usePerfilPrestador() {
  const [carga, setCarga] = useState<CargaPerfilPrestador>({ estado: 'cargando' })
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vigente = true
    setCarga({ estado: 'cargando' })
    consultarPerfilPrestador()
      .then((perfil) => {
        if (vigente) setCarga({ estado: 'ok', perfil })
      })
      .catch(() => {
        if (vigente) setCarga({ estado: 'error' })
      })
    return () => {
      vigente = false
    }
  }, [intento])

  const recargar = useCallback(() => setIntento((n) => n + 1), [])
  /** Después de un PUT: el perfil devuelto, sin volver a consultar. */
  const reemplazar = useCallback((perfil: PerfilPrestador) => setCarga({ estado: 'ok', perfil }), [])
  return { carga, recargar, reemplazar }
}
