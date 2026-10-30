import { useTranslation } from 'react-i18next'
import type { EstadoPublicacion } from '../serviciosPrestadorApi'

/** Estado de publicación con texto, nunca solo con color (WCAG 1.4.1): «Pendiente de aprobación» (N21), etc. */
export function InsigniaEstadoServicio({ estado }: { estado: EstadoPublicacion }) {
  const { t } = useTranslation()
  return (
    <span className={`insignia-servicio insignia-servicio--${estado.toLowerCase()}`}>
      {t(`prestador.estados.${estado}`)}
    </span>
  )
}
