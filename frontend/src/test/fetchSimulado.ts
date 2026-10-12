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

/** Página de error HTML, como la que genera nginx en un 502 o 504 (sin ProblemDetail). */
export function respuestaHtml(status: number): Response {
  return new Response(`<html><body><h1>${status}</h1><hr><center>nginx</center></body></html>`, {
    status,
    headers: { 'Content-Type': 'text/html' },
  })
}

/**
 * Reemplaza fetch por rutas "MÉTODO /ruta" (con o sin la consulta). Devuelve el registro de solicitudes para verificar, por
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
    // Primero la URL exacta; si no, la ruta sin la consulta (el manejador recibe la URL completa).
    const manejador =
      rutas[`${solicitud.metodo} ${solicitud.url}`] ?? rutas[`${solicitud.metodo} ${solicitud.url.split('?')[0]}`]
    if (!manejador) {
      throw new Error(`fetch sin simulación: ${solicitud.metodo} ${solicitud.url}`)
    }
    return manejador(solicitud)
  })
  vi.stubGlobal('fetch', fetchSimulado)
  return { fetchSimulado, solicitudes }
}

// ------------------------------------------------------------------ catálogo (GET /api/v1/catalogo/oferta)

export const HOTEL = {
  tipo: 'Servicio',
  id: 's-hotel',
  nombre: 'Hotel Casa Tayrona',
  categoria: 'Alojamiento',
  descripcionCorta: 'Alojamiento boutique cerca del Parque de los Novios.',
  precio: 150000,
  imagen: '/imagenes/catalogo/hotel-casa-tayrona.webp',
  coordenada: { longitud: -74.2125, latitud: 11.2425 },
  distanciaKm: null,
  accesibilidad: ['MovilidadReducida'],
  estadoOperativo: null,
}

export const TOUR_CAFE = {
  ...HOTEL,
  id: 's-tour-cafe',
  nombre: 'Tour de Café en Minca',
  categoria: 'Tour',
  descripcionCorta: 'Recorrido guiado por fincas cafeteras.',
  precio: 80000,
  accesibilidad: ['Auditiva'],
}

export const CASCADA_CERRADA = {
  tipo: 'Atractivo',
  id: 'a-cascada',
  nombre: 'Cascada El Salto',
  categoria: 'Naturaleza',
  descripcionCorta: 'Sendero hasta una caída de agua cerca de Minca.',
  precio: null,
  imagen: '/imagenes/catalogo/cascada-el-salto.webp',
  coordenada: { longitud: -74.11, latitud: 11.15 },
  distanciaKm: null,
  accesibilidad: [],
  estadoOperativo: 'Cerrado_temporalmente',
}

export function paginaOferta(
  elementos: unknown[],
  { pagina = 0, tamano = 10, totalElementos = elementos.length }: { pagina?: number; tamano?: number; totalElementos?: number } = {},
) {
  return { elementos, pagina, tamano, totalElementos, totalPaginas: Math.ceil(totalElementos / tamano) }
}

/** Consulta (sin el prefijo de la ruta) de la última petición al catálogo. */
export function ultimaConsultaCatalogo(solicitudes: SolicitudSimulada[]): string {
  const catalogo = solicitudes.filter((s) => s.url.startsWith('/api/v1/catalogo/oferta'))
  return catalogo.length === 0 ? '' : (catalogo[catalogo.length - 1].url.split('?')[1] ?? '')
}
