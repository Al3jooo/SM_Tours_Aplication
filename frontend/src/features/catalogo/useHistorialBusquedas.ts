import { useCallback, useEffect, useState } from 'react'
import { consultarHistorial, registrarBusqueda, type BusquedaReciente, type CuerpoHistorial } from './historialApi'

/** RF60: «sus últimas 10 búsquedas». */
const MAXIMO_BUSQUEDAS = 10

/**
 * «Últimas búsquedas» del turista (RF60, DEC-95, DEC-96). Solo con `activo` (la sesión de un Turista) pide la lista.
 * `registrar` no se espera y su error no se muestra ni afecta la búsqueda; después de un 204 vuelve a pedir la lista,
 * porque la deduplicación, la normalización y la poda las decide el servidor. Si la lista no carga, queda la anterior
 * (al principio, ninguna: la región no se muestra).
 */
export function useHistorialBusquedas(activo: boolean) {
  const [elementos, setElementos] = useState<BusquedaReciente[]>([])
  const [version, setVersion] = useState(0)

  useEffect(() => {
    if (!activo) {
      setElementos([])
      return
    }
    let vigente = true
    consultarHistorial()
      .then(({ elementos: recientes }) => {
        if (vigente) setElementos(recientes.slice(0, MAXIMO_BUSQUEDAS))
      })
      .catch(() => {
        // Un 401 ya cerró la sesión; otro error deja la lista como estaba.
      })
    return () => {
      vigente = false
    }
  }, [activo, version])

  const registrar = useCallback((cuerpo: CuerpoHistorial) => {
    registrarBusqueda(cuerpo)
      .then(() => setVersion((n) => n + 1))
      .catch(() => {
        // DEC-95: el historial nunca interrumpe la búsqueda.
      })
  }, [])

  return { elementos: activo ? elementos : [], registrar }
}
