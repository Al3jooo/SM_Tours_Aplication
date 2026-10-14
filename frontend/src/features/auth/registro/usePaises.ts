import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { consultarPaises } from '../authApi'
import { nombresPaises, type OpcionPais } from './paises'

type EstadoPaises = { estado: 'cargando' } | { estado: 'ok'; codigos: string[] } | { estado: 'error' }

/** Países del registro (RF01): códigos del backend y nombres traducidos al idioma activo, que se reordenan al cambiarlo. */
export function usePaises() {
  const { i18n } = useTranslation()
  const [estado, setEstado] = useState<EstadoPaises>({ estado: 'cargando' })
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vigente = true
    setEstado({ estado: 'cargando' })
    consultarPaises()
      .then((codigos) => {
        if (vigente) setEstado({ estado: 'ok', codigos })
      })
      .catch(() => {
        if (vigente) setEstado({ estado: 'error' })
      })
    return () => {
      vigente = false
    }
  }, [intento])

  const opciones: OpcionPais[] = useMemo(
    () => (estado.estado === 'ok' ? nombresPaises(estado.codigos, i18n.language) : []),
    [estado, i18n.language],
  )
  const reintentar = useCallback(() => setIntento((n) => n + 1), [])
  return { estado: estado.estado, codigos: estado.estado === 'ok' ? estado.codigos : null, opciones, reintentar }
}
