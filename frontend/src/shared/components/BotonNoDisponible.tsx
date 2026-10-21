import type { ReactNode, Ref } from 'react'
import { useTranslation } from 'react-i18next'

/**
 * Acción visible pero deshabilitada, con el estado único de DEC-22 (sin «Próximamente»): borde discontinuo,
 * aria-disabled (sigue siendo enfocable), sin acción y «No disponible» solo para lectores de pantalla. WF-06 y WF-12.
 */
export function BotonNoDisponible({
  children,
  className = '',
  ref,
}: {
  children: ReactNode
  className?: string
  /** Para devolverle el foco (p. ej. después de inactivar, WF-12). */
  ref?: Ref<HTMLButtonElement>
}) {
  const { t } = useTranslation()
  return (
    <button
      ref={ref}
      type="button"
      className={`boton-secundario boton-no-disponible ${className}`.trim()}
      aria-disabled="true"
      onClick={(evento) => evento.preventDefault()}
    >
      {children}
      <span className="solo-lector"> ({t('comun.noDisponible')})</span>
    </button>
  )
}
