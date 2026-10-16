import { useEffect, useState } from 'react'
import type { ResultadoOferta } from '../catalogo/catalogoApi'
import { hoyBogota, sumarDias } from './fechasBogota'
import { CATEGORIAS_RESERVABLES, consultarResumen, type EstadoResumen } from './reservasApi'

/** Periodo de la etiqueta de las tarjetas: hoy y los 6 días siguientes («los próximos 7 días»). */
export const DIAS_RESUMEN = 7

/** Tour y Alojamiento son las categorías que ms-booking reserva (D2). */
export function esReservable(resultado: Pick<ResultadoOferta, 'tipo' | 'categoria'>): boolean {
  return resultado.tipo === 'Servicio' && resultado.categoria !== null && CATEGORIAS_RESERVABLES.includes(resultado.categoria)
}

/**
 * «Disponibilidad» de las tarjetas (RF11): una sola consulta por página al resumen de ms-booking, solo con los
 * servicios reservables. Mientras carga, si ms-booking no responde o si cambió la página, devuelve null y las tarjetas
 * siguen con «—».
 */
export function useDisponibilidadResumen(resultados: ResultadoOferta[] | null): Record<string, EstadoResumen> | null {
  const ids = (resultados ?? []).filter(esReservable).map((resultado) => resultado.id)
  const clave = ids.join(',')
  const [respuesta, setRespuesta] = useState<{ clave: string; estados: Record<string, EstadoResumen> } | null>(null)

  useEffect(() => {
    if (clave === '') return
    let vigente = true
    const hoy = hoyBogota()
    consultarResumen(clave.split(','), hoy, sumarDias(hoy, DIAS_RESUMEN - 1))
      .then((estados) => {
        if (vigente) setRespuesta({ clave, estados })
      })
      .catch(() => {
        // Sin resumen: las tarjetas muestran «—».
      })
    return () => {
      vigente = false
    }
  }, [clave])

  return respuesta && respuesta.clave === clave ? respuesta.estados : null
}
