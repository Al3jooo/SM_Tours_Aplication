import { solicitar } from '../../shared/api/httpClient'
import type { Aviso, PreguntaFrecuente, TipoAviso } from '../contenido/contenidoApi'
import type { PaginaCuentas } from './adminApi'

/**
 * Gestión del contenido público (`docs/sad/api/ms-catalog.md`, DEC-60), a través del gateway. Solo el Administrador
 * (RNF06, DEC-65). El listado de preguntas es el público (`listarPreguntas`): no hay `GET /admin/faq`.
 */
const BASE = '/api/v1/catalogo/admin'

export interface DatosPregunta {
  titulo: string
  cuerpo: string
}

/** Los textos van sin los espacios de los extremos, como los cuenta el contador y los guarda ms-catalog (DEC-61). */
function cuerpoPregunta(datos: DatosPregunta): DatosPregunta {
  return { titulo: datos.titulo.trim(), cuerpo: datos.cuerpo.trim() }
}

export function crearPregunta(datos: DatosPregunta): Promise<PreguntaFrecuente> {
  return solicitar<PreguntaFrecuente>(`${BASE}/faq`, { method: 'POST', cuerpo: cuerpoPregunta(datos) })
}

export function editarPregunta(id: string, datos: DatosPregunta): Promise<PreguntaFrecuente> {
  return solicitar<PreguntaFrecuente>(`${BASE}/faq/${encodeURIComponent(id)}`, {
    method: 'PUT',
    cuerpo: cuerpoPregunta(datos),
  })
}

/** 204 sin cuerpo: borrado físico (DEC-64). */
export function eliminarPregunta(id: string): Promise<void> {
  return solicitar<void>(`${BASE}/faq/${encodeURIComponent(id)}`, { method: 'DELETE' })
}

/** Todos los avisos, activos e inactivos, del más reciente al más antiguo. La página de la pantalla empieza en 1; la de la API, en 0. */
export function listarAvisos(pagina: number): Promise<PaginaCuentas<Aviso>> {
  return solicitar<PaginaCuentas<Aviso>>(`${BASE}/avisos?pagina=${Math.max(0, pagina - 1)}`)
}

export function publicarAviso(mensaje: string, tipo: TipoAviso): Promise<Aviso> {
  return solicitar<Aviso>(`${BASE}/avisos`, { method: 'POST', cuerpo: { mensaje: mensaje.trim(), tipo } })
}

/** UPDATE condicional (DEC-64): 409 ESTADO_CAMBIADO si ya estaba inactivo, 404 si no existe. */
export function despublicarAviso(id: string): Promise<Aviso> {
  return solicitar<Aviso>(`${BASE}/avisos/${encodeURIComponent(id)}/despublicacion`, { method: 'POST' })
}
