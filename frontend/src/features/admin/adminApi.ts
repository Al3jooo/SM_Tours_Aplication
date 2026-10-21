import { solicitar } from '../../shared/api/httpClient'

/** Contrato de «Administrar cuentas» (`docs/sad/api/ms-auth.md`, DEC-40), a través del gateway. Solo el Administrador. */
const BASE = '/api/v1/auth/admin'

/** Estado derivado del prestador (DEC-43). */
export const ESTADOS_PRESTADOR = ['Pendiente', 'Activo', 'Rechazado', 'Inactivo'] as const
export type EstadoPrestador = (typeof ESTADOS_PRESTADOR)[number]

/** Estados del turista (RF62, DEC-43). */
export const ESTADOS_TURISTA = ['Activo', 'Desactivado'] as const
export type EstadoTurista = (typeof ESTADOS_TURISTA)[number]

/** Página de un listado del Administrador: 10 elementos (DEC-41), `pagina` desde 0. */
export interface PaginaCuentas<T> {
  elementos: T[]
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

export interface PrestadorResumen {
  idUsuario: string
  representanteLegal: string
  rnt: string
  correoCorporativo: string
  /** aaaa-mm-dd: el modelo guarda el día, no la hora. */
  fechaRegistro: string
  estado: EstadoPrestador
}

export interface PrestadorDetalle extends PrestadorResumen {
  /** Opcional (decisión D2 de «Registrarse»). */
  razonSocial: string | null
  telefono: string
  aceptoPoliticas: boolean
  motivoRechazo: string | null
  revisadoEn: string | null
}

export interface TuristaResumen {
  idUsuario: string
  nombre: string
  correo: string
  /** ISO 3166-1 alfa-2 (decisión D5 de «Registrarse»); el nombre lo pone el navegador. */
  pais: string | null
  fechaRegistro: string
  estado: EstadoTurista
}

export interface FiltrosCuentas {
  estado?: string
  q?: string
}

/** La página de la pantalla empieza en 1; la de la API, en 0. */
function consulta(filtros: FiltrosCuentas, pagina: number): string {
  const parametros = new URLSearchParams()
  if (filtros.estado) parametros.set('estado', filtros.estado)
  if (filtros.q) parametros.set('q', filtros.q)
  parametros.set('pagina', String(Math.max(0, pagina - 1)))
  return parametros.toString()
}

export function listarPrestadores(filtros: FiltrosCuentas, pagina: number): Promise<PaginaCuentas<PrestadorResumen>> {
  return solicitar<PaginaCuentas<PrestadorResumen>>(`${BASE}/prestadores?${consulta(filtros, pagina)}`)
}

export function consultarPrestador(idUsuario: string): Promise<PrestadorDetalle> {
  return solicitar<PrestadorDetalle>(`${BASE}/prestadores/${encodeURIComponent(idUsuario)}`)
}

/** RF28. La justificación (solo al rechazar) va sin los espacios de los extremos, como la cuenta el backend (DEC-42). */
export function validarPrestador(
  idUsuario: string,
  decision: 'Aprobar' | 'Rechazar',
  justificacion?: string,
): Promise<PrestadorDetalle> {
  return solicitar<PrestadorDetalle>(`${BASE}/prestadores/${encodeURIComponent(idUsuario)}/validacion`, {
    method: 'POST',
    cuerpo: decision === 'Rechazar' ? { decision, justificacion: (justificacion ?? '').trim() } : { decision },
  })
}

/** RF22, sin motivo (DEC-38). */
export function inactivarPrestador(idUsuario: string): Promise<PrestadorDetalle> {
  return solicitar<PrestadorDetalle>(`${BASE}/prestadores/${encodeURIComponent(idUsuario)}/inactivacion`, {
    method: 'POST',
  })
}

export function listarTuristas(filtros: FiltrosCuentas, pagina: number): Promise<PaginaCuentas<TuristaResumen>> {
  return solicitar<PaginaCuentas<TuristaResumen>>(`${BASE}/turistas?${consulta(filtros, pagina)}`)
}

/** RF62, sin motivo (DEC-38). Responde el turista con la misma forma que el listado. */
export function desactivarTurista(idUsuario: string): Promise<TuristaResumen> {
  return solicitar<TuristaResumen>(`${BASE}/turistas/${encodeURIComponent(idUsuario)}/desactivacion`, {
    method: 'POST',
  })
}

export function reactivarTurista(idUsuario: string): Promise<TuristaResumen> {
  return solicitar<TuristaResumen>(`${BASE}/turistas/${encodeURIComponent(idUsuario)}/reactivacion`, {
    method: 'POST',
  })
}
