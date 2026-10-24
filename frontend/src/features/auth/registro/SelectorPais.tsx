import { useTranslation } from 'react-i18next'
import type { usePaises } from './usePaises'

/**
 * País de origen (RF01; también el perfil, RF53): lista con los códigos de GET /api/v1/auth/paises y sus nombres en el
 * idioma activo. Mientras carga queda deshabilitada; si falla, «Reintentar». Compartido por WF-02 y WF-16 (DEC-98).
 */
export function SelectorPais({
  id,
  etiqueta,
  paises,
  valor,
  onCambiar,
  error,
}: {
  id: string
  etiqueta: string
  paises: ReturnType<typeof usePaises>
  valor: string
  onCambiar: (codigo: string) => void
  /** Texto del error del campo, ya traducido. */
  error?: string
}) {
  const { t } = useTranslation()
  return (
    <div className="campo">
      <label htmlFor={id}>{etiqueta}</label>
      <select
        id={id}
        className="campo-lista-registro"
        value={valor}
        disabled={paises.estado !== 'ok'}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        autoComplete="country"
        onChange={(e) => onCambiar(e.target.value)}
      >
        <option value="">{paises.estado === 'cargando' ? t('registro.paisesCargando') : t('registro.paisElegir')}</option>
        {paises.opciones.map((pais) => (
          <option key={pais.codigo} value={pais.codigo}>
            {pais.nombre}
          </option>
        ))}
      </select>
      {paises.estado === 'error' && (
        <p className="registro__paises-error">
          {t('registro.paisesError')}{' '}
          <button type="button" className="boton-secundario" onClick={paises.reintentar}>
            {t('catalogo.busqueda.reintentar')}
          </button>
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
