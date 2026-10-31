import { useTranslation } from 'react-i18next'
import type { EstadoOperativo } from '../catalogoApi'
import '../catalogo.css'

/**
 * Estado operativo de un atractivo (RF33: «visible en la ficha del atractivo»): el mismo texto y el mismo estilo en
 * la tarjeta de WF-05, la ficha (WF-06 de atractivo) y el panel del Gestor (WF-24), DEC-232. El estado se lee en texto,
 * no solo por el color.
 */
export function InsigniaEstadoOperativo({ estado }: { estado: EstadoOperativo }) {
  const { t } = useTranslation()
  return (
    <p className={`insignia-estado insignia-estado--${estado}`}>
      <span className="solo-lector">{t('catalogo.tarjeta.estado')}: </span>
      {t(`catalogo.estados.${estado}`)}
    </p>
  )
}
