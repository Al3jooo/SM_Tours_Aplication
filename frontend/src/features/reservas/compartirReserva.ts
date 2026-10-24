/**
 * «Compartir» de una reserva Completada (RF49, DEC-81, DEC-100), solo en el frontend: un enlace directo a la ficha
 * pública del servicio (WF-06), con la hoja de compartir del sistema o, si no existe, copiado al portapapeles. Si
 * tampoco se puede copiar, quien llama muestra el enlace para copiarlo a mano (FA2).
 */

/** Lo que se usa del navegador; las pruebas lo reemplazan. */
export interface NavegadorCompartir {
  share?: (datos: { title?: string; text?: string; url?: string }) => Promise<void>
  clipboard?: { writeText: (texto: string) => Promise<void> }
}

/** compartido: la hoja del sistema terminó; cancelado: el turista la cerró (FA3, no es un error). */
export type ResultadoCompartir = 'compartido' | 'cancelado' | 'copiado' | 'manual'

/** Enlace absoluto a WF-06 con el origen de la aplicación. */
export function enlaceServicio(idServicio: string, origen: string = window.location.origin): string {
  return `${origen}/servicios/${encodeURIComponent(idServicio)}`
}

const esCancelacion = (error: unknown) => error instanceof DOMException && error.name === 'AbortError'

export async function compartirEnlace(
  titulo: string,
  url: string,
  navegador: NavegadorCompartir = globalThis.navigator,
): Promise<ResultadoCompartir> {
  if (typeof navegador?.share === 'function') {
    try {
      await navegador.share({ title: titulo, url })
      return 'compartido'
    } catch (error) {
      if (esCancelacion(error)) return 'cancelado'
      // Otro error (p. ej. NotAllowedError): se intenta el portapapeles.
    }
  }
  if (typeof navegador?.clipboard?.writeText === 'function') {
    try {
      await navegador.clipboard.writeText(url)
      return 'copiado'
    } catch {
      // Sin permiso para escribir en el portapapeles: queda el enlace a mano.
    }
  }
  return 'manual'
}
