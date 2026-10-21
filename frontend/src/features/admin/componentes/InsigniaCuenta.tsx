import { useTranslation } from 'react-i18next'
import type { EstadoPrestador, EstadoTurista } from '../adminApi'

/** Estado de la cuenta con texto (no solo color, WCAG 1.4.1) y los pares de color ya verificados (DEC-53). */
export function InsigniaCuenta({ estado }: { estado: EstadoPrestador | EstadoTurista }) {
  const { t } = useTranslation()
  return <span className={`insignia-cuenta insignia-cuenta--${estado}`}>{t(`admin.estados.${estado}`)}</span>
}
