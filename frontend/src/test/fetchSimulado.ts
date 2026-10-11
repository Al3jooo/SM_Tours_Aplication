import { vi } from 'vitest'

export interface SolicitudSimulada {
  url: string
  metodo: string
  cabeceras: Record<string, string>
  cuerpo: unknown
}

type Manejador = (solicitud: SolicitudSimulada) => Response | Promise<Response>

export const USUARIO_TURISTA = {
  idUsuario: '00000000-0000-4000-8000-000000000001',
  correo: 'turista@samaria360.test',
  rol: 'Turista',
  estadoCuenta: 'Activo',
  nombreVisible: 'Ana Pérez',
}

export const SESION_TURISTA = {
  token: 'token-de-prueba',
  tipo: 'Bearer',
  expiraEn: '2026-10-11T05:00:00Z',
  idUsuario: USUARIO_TURISTA.idUsuario,
  rol: 'Turista',
}

export function respuestaJson(status: number, cuerpo: unknown): Response {
  return new Response(JSON.stringify(cuerpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

/** ProblemDetail (RFC 9457) como los que responde ms-auth. */
export function respuestaProblema(status: number, codigo: string, extra: Record<string, unknown> = {}): Response {
  return new Response(JSON.stringify({ type: 'about:blank', status, detail: codigo, codigo, ...extra }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

/**
 * Reemplaza fetch por rutas "MÉTODO /ruta". Devuelve el registro de solicitudes para verificar, por
 * ejemplo, la cabecera Authorization.
 */
export function simularFetch(rutas: Record<string, Manejador>) {
  const solicitudes: SolicitudSimulada[] = []
  const fetchSimulado = vi.fn(async (entrada: RequestInfo | URL, init?: RequestInit) => {
    const solicitud: SolicitudSimulada = {
      url: String(entrada),
      metodo: init?.method ?? 'GET',
      cabeceras: { ...(init?.headers as Record<string, string> | undefined) },
      cuerpo: typeof init?.body === 'string' ? JSON.parse(init.body) : undefined,
    }
    solicitudes.push(solicitud)
    const manejador = rutas[`${solicitud.metodo} ${solicitud.url}`]
    if (!manejador) {
      throw new Error(`fetch sin simulación: ${solicitud.metodo} ${solicitud.url}`)
    }
    return manejador(solicitud)
  })
  vi.stubGlobal('fetch', fetchSimulado)
  return { fetchSimulado, solicitudes }
}
