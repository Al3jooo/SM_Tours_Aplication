import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

/**
 * Elemento del wireframe fuera del alcance de esta iteración: visible pero deshabilitado.
 * Usa aria-disabled (no disabled) para que siga siendo enfocable con el teclado y el lector de
 * pantalla anuncie "Próximamente".
 */
export function BotonProximamente({
  children,
  className = '',
  etiquetaVisible = true,
  etiquetaAccesible,
}: {
  children: ReactNode
  className?: string
  /** false: "Próximamente" solo para lectores de pantalla y como tooltip. */
  etiquetaVisible?: boolean
  /** Para botones solo con icono. */
  etiquetaAccesible?: string
}) {
  const { t } = useTranslation()
  const proximamente = t('comun.proximamente')
  return (
    <button
      type="button"
      className={`deshabilitado ${className}`.trim()}
      aria-disabled="true"
      aria-label={etiquetaAccesible ? `${etiquetaAccesible} (${proximamente})` : undefined}
      title={proximamente}
      onClick={(evento) => evento.preventDefault()}
    >
      {children}
      {!etiquetaAccesible && (
        <span className={etiquetaVisible ? 'insignia-proximamente' : 'solo-lector'}>
          {etiquetaVisible ? proximamente : ` (${proximamente})`}
        </span>
      )}
    </button>
  )
}
