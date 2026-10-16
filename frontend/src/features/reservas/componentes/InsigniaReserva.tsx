import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { IconoCancelada, IconoCompletada, IconoConfirmada } from '../../../shared/components/Iconos'
import type { EstadoReserva } from '../reservasApi'

const ICONOS: Record<EstadoReserva, () => ReactNode> = {
  Confirmada: IconoConfirmada,
  Cancelada: IconoCancelada,
  Completada: IconoCompletada,
}

/** Estado de una reserva con texto e icono (nunca solo con el color): Confirmada, Cancelada o Completada (RF52). */
export function InsigniaReserva({ estado }: { estado: EstadoReserva }) {
  const { t } = useTranslation()
  const Icono = ICONOS[estado]
  return (
    <p className={`insignia-reserva insignia-reserva--${estado}`}>
      <Icono />
      <span className="solo-lector">{t('misReservas.estado')}: </span>
      {t(`misReservas.estados.${estado}`)}
    </p>
  )
}
