import { createContext } from 'react'
import type { Favorito, TipoFavorito } from './favoritosApi'

export type EstadoFavoritos = 'sinCargar' | 'cargando' | 'ok' | 'error'

/**
 * Cómo se muestra el corazón (DEC-93): activo para el Turista; para un visitante, lleva al login; con otro rol o
 * mientras se valida la sesión, no se muestra (null).
 */
export type ModoFavorito = 'turista' | 'visitante' | null

/** Lo que hace falta para marcar un elemento y nombrarlo en el corazón y en los anuncios. */
export interface ReferenciaFavorito {
  tipo: TipoFavorito
  id: string
  nombre: string
}

/** Anuncio de la región `role="status"` (AvisoFavoritos): una clave i18n; `n` cambia para repetir el mismo texto. */
export interface AnuncioFavoritos {
  clave: string
  error: boolean
  n: number
}

export interface ValorFavoritos {
  modo: ModoFavorito
  estado: EstadoFavoritos
  /** Del más reciente al más antiguo, con los cambios locales aún sin confirmar. */
  elementos: Favorito[]
  /** Carga la lista si todavía no se pidió o si la última vez falló (DEC-93: un fallo no queda fijo). */
  asegurarCarga: () => void
  esFavorito: (tipo: TipoFavorito, id: string) => boolean
  pendiente: (tipo: TipoFavorito, id: string) => boolean
  /** Marca o desmarca; resuelve con el estado final (true = es favorito). */
  alternar: (referencia: ReferenciaFavorito) => Promise<boolean>
  /** «Quitar» de WF-17; resuelve con true si se quitó. */
  quitar: (tipo: TipoFavorito, id: string) => Promise<boolean>
  anuncio: AnuncioFavoritos | null
  limpiarAnuncio: () => void
}

export const FavoritosContext = createContext<ValorFavoritos | null>(null)

export const claveFavorito = (tipo: TipoFavorito, id: string) => `${tipo}:${id}`
