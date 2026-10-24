import { useContext, useEffect } from 'react'
import { FavoritosContext, type ValorFavoritos } from './FavoritosContext'

/**
 * Favoritos de la sesión para un consumidor (corazón o «Mis favoritos»). Al montar pide la lista si hace falta: la
 * primera vez, o de nuevo si la anterior falló (DEC-93). Fuera de `FavoritosProvider` devuelve null y no hay corazón.
 */
export function useFavoritos(): ValorFavoritos | null {
  const favoritos = useContext(FavoritosContext)
  const asegurarCarga = favoritos?.modo === 'turista' ? favoritos.asegurarCarga : null
  useEffect(() => {
    asegurarCarga?.()
  }, [asegurarCarga])
  return favoritos
}
