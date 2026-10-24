import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router-dom'
import { IconoCorazon } from '../../shared/components/Iconos'
import type { ReferenciaFavorito } from './FavoritosContext'
import { useFavoritos } from './useFavoritos'
import './favoritos.css'

/**
 * Corazón «Guardar en favoritos» (RF54, DEC-93) de las tarjetas de WF-05 y de la ficha WF-06:
 * - Turista: `aria-pressed` refleja si es favorito; el nombre accesible incluye el del elemento y no cambia;
 * - visitante: lleva al login y vuelve aquí (no marca nada solo al volver);
 * - otro rol, o mientras se valida la sesión: no se muestra;
 * - sin la lista (cargando o con error): visible con aria-disabled y, si falló, el motivo para lectores de pantalla.
 * Los anuncios van en la región de AvisoFavoritos de la pantalla.
 */
export function BotonFavorito({
  referencia,
  conTexto = false,
  className = '',
}: {
  referencia: ReferenciaFavorito
  /** WF-06: «Guardar en favoritos» visible junto al icono. */
  conTexto?: boolean
  className?: string
}) {
  const { t } = useTranslation()
  const id = useId()
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const favoritos = useFavoritos()
  if (!favoritos?.modo) return null

  const etiqueta = t('favoritos.boton', { nombre: referencia.nombre })
  const clases = `boton-favorito${conTexto ? ' boton-favorito--texto' : ''} ${className}`.trim()
  const contenido = (lleno: boolean) => (
    <>
      <IconoCorazon lleno={lleno} />
      {conTexto && <span aria-hidden="true">{t('detalle.favorito')}</span>}
    </>
  )

  if (favoritos.modo === 'visitante') {
    return (
      <button
        type="button"
        className={clases}
        aria-label={etiqueta}
        aria-describedby={`${id}-sesion`}
        onClick={() =>
          navegar('/login', { state: { desde: ubicacion.pathname + ubicacion.search + ubicacion.hash } })
        }
      >
        {contenido(false)}
        <span id={`${id}-sesion`} className="solo-lector">
          {t('favoritos.requiereSesion')}
        </span>
      </button>
    )
  }

  const { tipo, id: idElemento } = referencia
  const listo = favoritos.estado === 'ok'
  const marcado = listo && favoritos.esFavorito(tipo, idElemento)
  const pendiente = favoritos.pendiente(tipo, idElemento)
  return (
    <button
      type="button"
      className={clases}
      aria-label={etiqueta}
      aria-pressed={marcado}
      aria-disabled={listo ? undefined : true}
      aria-busy={pendiente || undefined}
      aria-describedby={favoritos.estado === 'error' ? `${id}-error` : undefined}
      onClick={(evento) => {
        if (!listo || pendiente) {
          evento.preventDefault()
          return
        }
        void favoritos.alternar(referencia)
      }}
    >
      {contenido(marcado)}
      {favoritos.estado === 'error' && (
        <span id={`${id}-error`} className="solo-lector">
          {t('favoritos.noCargados')}
        </span>
      )}
    </button>
  )
}
