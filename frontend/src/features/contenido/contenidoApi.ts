import { solicitar } from '../../shared/api/httpClient'

/** Contrato público de «Contenido público» (`docs/sad/api/ms-catalog.md`, DEC-60), a través del gateway. */
const BASE = '/api/v1/catalogo'

/** Pregunta frecuente (RF26): título de 1 a 150 y cuerpo de 1 a 2000, en el idioma en que se escribió (DEC-61, DEC-69). */
export interface PreguntaFrecuente {
  id: string
  titulo: string
  cuerpo: string
  /** Instante ISO 8601 en UTC. */
  fechaActualizacion: string
}

/** Tipos de aviso de RF40 (DEC-58); el valor es el de la API y la etiqueta sale de i18n. */
export const TIPOS_AVISO = ['Clima', 'Cierre', 'Comunicado'] as const
export type TipoAviso = (typeof TIPOS_AVISO)[number]

export interface Aviso {
  id: string
  mensaje: string
  tipo: TipoAviso
  activo: boolean
  /** Instante ISO 8601 en UTC. */
  fechaPublicacion: string
}

interface Lista<T> {
  elementos: T[]
}

/**
 * Todas las preguntas, sin paginar (como mucho 200, DEC-62), en el orden del servidor: alfabético por título (DEC-63).
 * Endpoint público: sale sin token (DEC-47). El Administrador usa este mismo listado (no hay `GET /admin/faq`).
 */
export function listarPreguntas(): Promise<PreguntaFrecuente[]> {
  return solicitar<Lista<PreguntaFrecuente>>(`${BASE}/faq`, { publica: true }).then((lista) => lista.elementos)
}

/** Avisos activos del banner, del más reciente al más antiguo (RF40). Público y sin token (DEC-47). */
export function listarAvisosActivos(): Promise<Aviso[]> {
  return solicitar<Lista<Aviso>>(`${BASE}/avisos`, { publica: true }).then((lista) => lista.elementos)
}

type OyenteAvisos = () => void
const oyentes = new Set<OyenteAvisos>()

/** El banner se suscribe para volver a pedir los avisos cuando cambian (DEC-72). Devuelve la función para desuscribirse. */
export function alCambiarAvisos(oyente: OyenteAvisos): () => void {
  oyentes.add(oyente)
  return () => {
    oyentes.delete(oyente)
  }
}

/** La gestión de avisos (WF-14) lo llama después de publicar o despublicar: el banner se actualiza sin recargar. */
export function avisarCambioAvisos(): void {
  oyentes.forEach((oyente) => oyente())
}
