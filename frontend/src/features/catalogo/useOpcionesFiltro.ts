import { useCallback, useEffect, useState } from 'react'
import type { Categoria } from './catalogoApi'
import { consultarOpcionesFiltro, type OpcionesFiltro } from './opcionesFiltroApi'

export type EstadoOpciones =
  | { estado: 'inactivo' }
  | { estado: 'cargando' }
  | { estado: 'ok'; opciones: OpcionesFiltro }
  | { estado: 'error' }

/**
 * Opciones de «Más filtros» de la categoría elegida en el panel. Solo se piden con el panel abierto y una categoría;
 * al cambiar de categoría se descarta la respuesta anterior. Las ya recibidas se guardan mientras el panel exista.
 */
export function useOpcionesFiltro(categoria: Categoria | undefined, activo: boolean) {
  const [recibidas, setRecibidas] = useState<Partial<Record<Categoria, OpcionesFiltro>>>({})
  const [fallida, setFallida] = useState<Categoria | null>(null)
  const [intento, setIntento] = useState(0)
  const guardadas = categoria ? recibidas[categoria] : undefined

  useEffect(() => {
    if (!activo || !categoria || guardadas) return
    let vigente = true
    setFallida(null)
    consultarOpcionesFiltro(categoria)
      .then((opciones) => {
        if (vigente) setRecibidas((actuales) => ({ ...actuales, [categoria]: opciones }))
      })
      .catch(() => {
        if (vigente) setFallida(categoria)
      })
    return () => {
      vigente = false
    }
  }, [activo, categoria, guardadas, intento])

  const reintentar = useCallback(() => setIntento((n) => n + 1), [])

  let estado: EstadoOpciones
  if (!activo || !categoria) estado = { estado: 'inactivo' }
  else if (guardadas) estado = { estado: 'ok', opciones: guardadas }
  else if (fallida === categoria) estado = { estado: 'error' }
  else estado = { estado: 'cargando' }
  return { ...estado, reintentar } as EstadoOpciones & { reintentar: () => void }
}
