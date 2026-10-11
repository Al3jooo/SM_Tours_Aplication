import { useTranslation } from 'react-i18next'
import { IconoAsistente } from './Iconos'
import { BotonProximamente } from './Proximamente'

/** Botón flotante del asistente virtual (RF04), común a todas las pantallas. Fuera de alcance por ahora. */
export function BotonAsistente() {
  const { t } = useTranslation()
  return (
    <div className="boton-asistente">
      <BotonProximamente>
        <IconoAsistente />
        <span>{t('asistente.boton')}</span>
      </BotonProximamente>
    </div>
  )
}
