import { leerToken } from './tokenSesion'

/** Cuerpo RFC 9457 que devuelven los microservicios, con el campo `codigo` estable. */
export interface ProblemDetail {
  status?: number
  title?: string
  detail?: string
  codigo?: string
  [propiedad: string]: unknown
}

export const ERROR_RED = 'ERROR_RED'

export class ApiError extends Error {
  readonly status: number
  readonly codigo: string
  readonly problema: ProblemDetail | null

  constructor(status: number, codigo: string, problema: ProblemDetail | null) {
    super(problema?.detail ?? codigo)
    this.name = 'ApiError'
    this.status = status
    this.codigo = codigo
    this.problema = problema
  }
}

type OyenteSesionExpirada = (error: ApiError) => void
const oyentes = new Set<OyenteSesionExpirada>()

/** Avisa cuando una petición autenticada revela que la sesión ya no es válida. Devuelve la función para desuscribirse. */
export function alExpirarSesion(oyente: OyenteSesionExpirada): () => void {
  oyentes.add(oyente)
  return () => {
    oyentes.delete(oyente)
  }
}

interface OpcionesSolicitud {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  cuerpo?: unknown
}

/**
 * Cliente HTTP de la aplicación. Las rutas son siempre relativas (/api/v1/...): en desarrollo las
 * atiende el proxy de Vite y en despliegue el API Gateway.
 */
export async function solicitar<T>(ruta: string, opciones: OpcionesSolicitud = {}): Promise<T> {
  const token = leerToken()
  const cabeceras: Record<string, string> = { Accept: 'application/json' }
  if (opciones.cuerpo !== undefined) {
    cabeceras['Content-Type'] = 'application/json'
  }
  if (token) {
    cabeceras.Authorization = `Bearer ${token}`
  }

  let respuesta: Response
  try {
    respuesta = await fetch(ruta, {
      method: opciones.method ?? 'GET',
      headers: cabeceras,
      body: opciones.cuerpo === undefined ? undefined : JSON.stringify(opciones.cuerpo),
    })
  } catch {
    throw new ApiError(0, ERROR_RED, null)
  }

  if (!respuesta.ok) {
    const problema = await leerProblema(respuesta)
    const error = new ApiError(respuesta.status, problema?.codigo ?? `HTTP_${respuesta.status}`, problema)
    if (token && sesionInvalida(error)) {
      oyentes.forEach((oyente) => oyente(error))
    }
    throw error
  }

  return (await respuesta.json()) as T
}

/** 401, o 403 por cuenta no activa (RF22, RF62). Un 403 por rol (ACCESO_DENEGADO) no cierra la sesión. */
export function sesionInvalida(error: ApiError): boolean {
  return error.status === 401 || (error.status === 403 && error.codigo === 'CUENTA_INACTIVA')
}

async function leerProblema(respuesta: Response): Promise<ProblemDetail | null> {
  try {
    return (await respuesta.json()) as ProblemDetail
  } catch {
    return null
  }
}
