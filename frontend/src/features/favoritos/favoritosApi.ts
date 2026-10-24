import { solicitar } from '../../shared/api/httpClient'

/** Contrato de los favoritos de ms-catalog (docs/sad/api/ms-catalog.md, «Favoritos»; RF54, DEC-80, DEC-86). */

const BASE = '/api/v1/catalogo/favoritos'

export type TipoFavorito = 'Servicio' | 'Atractivo'

export interface Favorito {
  tipo: TipoFavorito
  id: string
  /** Null si el documento ya no existe en el catálogo. */
  nombre: string | null
  categoria: string | null
  imagen: string | null
  fechaCreacion: string
  /** Un servicio Publicado de un prestador activo, o un atractivo que existe. */
  disponible: boolean
}

/** Tope por turista (DEC-86): «n de 100 favoritos». */
export const LIMITE_FAVORITOS = 100

const ruta = (tipo: TipoFavorito, id: string) => `${BASE}/${tipo}/${encodeURIComponent(id)}`

/** Todos (como máximo 100), del más reciente al más antiguo. Solo el rol Turista: lleva el token. */
export function listarFavoritos(): Promise<{ elementos: Favorito[] }> {
  return solicitar<{ elementos: Favorito[] }>(BASE)
}

/** 201 si se creó, 200 si ya estaba; el cuerpo es el favorito resuelto. */
export function guardarFavorito(tipo: TipoFavorito, id: string): Promise<Favorito> {
  return solicitar<Favorito>(ruta(tipo, id), { method: 'PUT' })
}

/** 204 siempre (idempotente). */
export async function quitarFavorito(tipo: TipoFavorito, id: string): Promise<void> {
  await solicitar<void>(ruta(tipo, id), { method: 'DELETE' })
}
