import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { IconoUbicacion } from '../../../shared/components/Iconos'
import type { EstadoUbicacion } from '../useUbicacion'

/**
 * «Centrar en mi ubicación». Es lo único que pide la ubicación al navegador; con la ubicación ya obtenida, solo
 * vuelve a centrar el mapa. Sin geolocalización en el navegador queda deshabilitado (aria-disabled) con la razón.
 */
export function BotonUbicacion({ estado, onCentrar }: { estado: EstadoUbicacion['estado']; onCentrar: () => void }) {
  const { t } = useTranslation()
  const id = useId()
  const noSoportada = estado === 'noSoportada'
  const solicitando = estado === 'solicitando'

  return (
    <div className="boton-ubicacion">
      <button
        type="button"
        className={`boton-secundario${noSoportada ? ' deshabilitado' : ''}`}
        aria-disabled={noSoportada || undefined}
        aria-describedby={noSoportada ? `${id}-sin-soporte` : undefined}
        aria-busy={solicitando || undefined}
        onClick={noSoportada ? undefined : onCentrar}
      >
        <IconoUbicacion />
        <span>{t(solicitando ? 'ubicacion.obteniendo' : 'ubicacion.centrar')}</span>
      </button>
      {noSoportada && (
        <p id={`${id}-sin-soporte`} className="filtro__ayuda">
          {t('ubicacion.noSoportada')}
        </p>
      )}
    </div>
  )
}
