import { solicitar } from '../../shared/api/httpClient'
import type { EventoAgenda, PaginaEventos } from '../agenda/agendaApi'

/**
 * Contrato de `/api/v1/catalogo/gestor/eventos` (ms-catalog, iteración E, A2; `EventoGestorController`;
 * docs/sad/api/ms-catalog.md): solo el rol Gestor (DEC-220). Registrar y listar; sin edición ni borrado (DEC-215).
 */
export interface EventoGestion extends EventoAgenda {
  gestorId: string
  /** Instante ISO 8601 en UTC. */
  fechaRegistro: string
}

/** El cuerpo de `SolicitudEvento`: las fechas como texto `yyyy-MM-dd` (DEC-236). */
export interface DatosEvento {
  nombre: string
  fechaInicio: string
  fechaFin: string
  ubicacion: string
  descripcion: string
}

const RUTA = '/api/v1/catalogo/gestor/eventos'

/** 201 con el evento: queda publicado de inmediato en la agenda (RF25). */
export function registrarEvento(datos: DatosEvento): Promise<EventoGestion> {
  return solicitar<EventoGestion>(RUTA, { method: 'POST', cuerpo: datos })
}

/** Panel de WF-25 (DEC-238): todos los eventos, también los vencidos. `pagina` desde 1 en la pantalla, desde 0 en la API. */
export function listarEventos(pagina: number): Promise<PaginaEventos<EventoGestion>> {
  return solicitar<PaginaEventos<EventoGestion>>(`${RUTA}?pagina=${Math.max(0, pagina - 1)}`)
}
