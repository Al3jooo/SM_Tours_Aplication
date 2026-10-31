import type { InputHTMLAttributes, ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { longitudTexto } from '../../admin/textoAcotado'
import { LONGITUD_MAXIMA, idDeCampo, rutaSinIndices } from '../rutasCampos'

/**
 * Campo de WF-08 con el patrón visual de `Campo` (`.campo`, `.campo__control`): el `id` sale de la ruta del contrato
 * (DEC-166), así que el error del cliente y el del servidor quedan junto al mismo control. Los textos con longitud
 * máxima llevan contador (como WF-13); el contador y el error se asocian con `aria-describedby`.
 */
export function CampoServicio({
  ruta,
  etiqueta,
  valor,
  onCambiar,
  error,
  opcional = false,
  multilinea = false,
  ayuda,
  sufijo,
  maximo: maximoPropio,
  ...entrada
}: {
  ruta: string
  etiqueta: string
  valor: string
  onCambiar: (valor: string) => void
  error?: string
  opcional?: boolean
  multilinea?: boolean
  ayuda?: string
  /** Texto fijo después del control (p. ej. «COP»). */
  sufijo?: ReactNode
  /**
   * Máximo del contador cuando no es el de WF-08 (p. ej. la descripción de un atractivo, 1000 y no 2000; DEC-227).
   * Sin él, el de `LONGITUD_MAXIMA` por la ruta.
   */
  maximo?: number
} & Omit<InputHTMLAttributes<HTMLInputElement>, 'id' | 'value' | 'onChange'>) {
  const { t } = useTranslation()
  const id = idDeCampo(ruta)
  const maximo = maximoPropio ?? LONGITUD_MAXIMA[rutaSinIndices(ruta)]
  const longitud = longitudTexto(valor)
  const descripcion =
    [ayuda && `${id}-ayuda`, error && `${id}-error`, maximo && `${id}-contador`].filter(Boolean).join(' ') || undefined
  const comunes = {
    id,
    value: valor,
    'aria-invalid': error ? true : undefined,
    'aria-describedby': descripcion,
  }

  return (
    <div className="campo">
      <label htmlFor={id}>
        {etiqueta}
        {opcional && <span className="campo__opcional"> {t('registro.opcional')}</span>}
      </label>
      {multilinea ? (
        <textarea
          {...comunes}
          className={`prestador__texto${error ? ' prestador__texto--error' : ''}`}
          rows={4}
          onChange={(evento) => onCambiar(evento.target.value)}
        />
      ) : (
        <div className="campo__control">
          <input type="text" {...entrada} {...comunes} onChange={(evento) => onCambiar(evento.target.value)} />
          {sufijo && <span className="prestador__sufijo">{sufijo}</span>}
        </div>
      )}
      {ayuda && (
        <p id={`${id}-ayuda`} className="campo__ayuda">
          {ayuda}
        </p>
      )}
      {maximo && (
        <p id={`${id}-contador`} className={`prestador__contador${longitud > maximo ? ' prestador__contador--excedido' : ''}`}>
          {t('prestador.registro.contador', { n: longitud, maximo })}
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

/** Error de un grupo (fieldset): el id sigue la misma ruta que el de un control. */
export function ErrorGrupo({ ruta, error }: { ruta: string; error?: string }) {
  if (!error) return null
  return (
    <p id={`${idDeCampo(ruta)}-error`} className="campo__error">
      {error}
    </p>
  )
}
