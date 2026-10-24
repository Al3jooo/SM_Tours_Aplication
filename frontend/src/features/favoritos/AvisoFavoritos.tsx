import { useContext, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { FavoritosContext } from './FavoritosContext'
import './favoritos.css'

/**
 * Región `role="status"` de los favoritos (DEC-93): «Guardado en favoritos», «Quitado de favoritos» o el error. Va una
 * sola vez en cada pantalla con corazones o con «Quitar» (WF-05, WF-06 y WF-17) y es la única que dice estos mensajes.
 * Al salir de la pantalla, el anuncio se descarta para que no aparezca en la siguiente.
 */
export function AvisoFavoritos() {
  const { t } = useTranslation()
  const favoritos = useContext(FavoritosContext)
  const limpiar = favoritos?.limpiarAnuncio
  useEffect(() => () => limpiar?.(), [limpiar])
  if (!favoritos) return null
  const { anuncio } = favoritos
  return (
    <p className={`aviso-favoritos${anuncio?.error ? ' aviso-favoritos--error' : ''}`} role="status">
      {anuncio && <span key={anuncio.n}>{t(anuncio.clave)}</span>}
    </p>
  )
}
