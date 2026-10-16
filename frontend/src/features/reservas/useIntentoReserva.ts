import { useCallback, useState } from 'react'
import { ApiError, type ProblemDetail } from '../../shared/api/httpClient'
import { claveParaIntento, terminarIntento } from './intentoReserva'
import { cuerpoReserva, reservar, type PedidoReserva, type Reserva } from './reservasApi'

export type EstadoEnvio =
  | { estado: 'inactivo' }
  | { estado: 'enviando' }
  | { estado: 'confirmada'; reserva: Reserva }
  | { estado: 'error'; status: number; codigo: string; problema: ProblemDetail | null }

/**
 * Confirmar la reserva (RF20) con la Idempotency-Key del intento (D6): la misma selección reintentada (después de un
 * error de red, un 503 o una recarga) sale con la misma clave; otra selección o un precio nuevo, con otra.
 */
export function useIntentoReserva() {
  const [envio, setEnvio] = useState<EstadoEnvio>({ estado: 'inactivo' })

  const confirmar = useCallback(async (idUsuario: string, pedido: PedidoReserva) => {
    const cuerpo = cuerpoReserva(pedido)
    const clave = claveParaIntento(idUsuario, cuerpo)
    setEnvio({ estado: 'enviando' })
    try {
      const reserva = await reservar(cuerpo, clave)
      terminarIntento(201)
      setEnvio({ estado: 'confirmada', reserva })
    } catch (error: unknown) {
      const fallo = error instanceof ApiError ? error : new ApiError(500, 'GENERICO', null)
      terminarIntento(fallo.status)
      setEnvio({ estado: 'error', status: fallo.status, codigo: fallo.codigo, problema: fallo.problema })
    }
  }, [])

  const descartar = useCallback(() => setEnvio({ estado: 'inactivo' }), [])
  return { envio, confirmar, descartar }
}
