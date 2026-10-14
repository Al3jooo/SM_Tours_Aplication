import type { InputHTMLAttributes, ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

/**
 * Campo de texto del registro con el patrón visual del login (`.campo`, `.campo__control`): etiqueta, icono decorativo,
 * ayuda visible y error asociados con `aria-describedby`, y `aria-invalid`.
 */
export function Campo({
  id,
  etiqueta,
  opcional = false,
  icono,
  ayuda,
  error,
  ...entrada
}: {
  id: string
  etiqueta: string
  opcional?: boolean
  icono?: ReactNode
  /** Instrucción visible que no desaparece al escribir (no un placeholder), conectada con aria-describedby. */
  ayuda?: string
  error?: string
} & Omit<InputHTMLAttributes<HTMLInputElement>, 'id'>) {
  const { t } = useTranslation()
  const descripcion = [ayuda && `${id}-ayuda`, error && `${id}-error`].filter(Boolean).join(' ') || undefined
  return (
    <div className="campo">
      <label htmlFor={id}>
        {etiqueta}
        {opcional && <span className="campo__opcional"> {t('registro.opcional')}</span>}
      </label>
      <div className="campo__control">
        {icono}
        <input
          id={id}
          aria-invalid={error ? true : undefined}
          aria-describedby={descripcion}
          {...entrada}
        />
      </div>
      {ayuda && (
        <p id={`${id}-ayuda`} className="campo__ayuda">
          {ayuda}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="campo__error">
          {error}
        </p>
      )}
    </div>
  )
}
