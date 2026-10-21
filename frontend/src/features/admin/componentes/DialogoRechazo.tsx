import { useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { longitudJustificacion, MAXIMO_JUSTIFICACION, validarJustificacion } from '../justificacion'
import { DialogoAdmin } from './DialogoAdmin'

/**
 * «Rechazar prestador» (RF28, FA1): justificación obligatoria de 1 a 500 caracteres sin los espacios de los extremos
 * (DEC-42). «Confirmar rechazo» queda deshabilitado (aria-disabled, sigue enfocable) mientras no sea válida; el 400
 * `campos.justificacion` del servidor se muestra en el campo. Avisa que la decisión es definitiva.
 */
export function DialogoRechazo({
  nombre,
  enviando,
  errorCampo,
  error,
  onConfirmar,
  onCerrar,
}: {
  nombre: string
  enviando: boolean
  /** Código de `campos.justificacion` del último 400. */
  errorCampo: string | null
  /** Código de otro error del último intento (red, 503…). */
  error: string | null
  onConfirmar: (justificacion: string) => void
  onCerrar: () => void
}) {
  const { t } = useTranslation()
  const id = useId()
  const campo = useRef<HTMLTextAreaElement>(null)
  const [texto, setTexto] = useState('')
  // El error del servidor vale hasta que se vuelve a escribir.
  const [errorServidor, setErrorServidor] = useState(errorCampo)
  const [ultimoErrorCampo, setUltimoErrorCampo] = useState(errorCampo)
  if (errorCampo !== ultimoErrorCampo) {
    setUltimoErrorCampo(errorCampo)
    setErrorServidor(errorCampo)
  }

  const longitud = longitudJustificacion(texto)
  const invalida = validarJustificacion(texto)
  // Más de 500 se marca mientras se escribe; vacía solo deshabilita «Confirmar» (y la marca el 400 si llegara).
  const errorMostrado = errorServidor ?? (invalida === 'LONGITUD_MAXIMA' ? invalida : null)
  const bloqueado = enviando || invalida !== null
  const describe = [`${id}-contador`, errorMostrado ? `${id}-error` : null].filter(Boolean).join(' ')

  return (
    <DialogoAdmin
      titulo={t('admin.rechazo.titulo', { nombre })}
      bloqueado={enviando}
      focoInicial={campo}
      onCerrar={onCerrar}
    >
      {(idTexto) => (
        <form
          noValidate
          onSubmit={(evento) => {
            evento.preventDefault()
            if (!bloqueado) onConfirmar(texto)
          }}
        >
          <p id={idTexto}>{t('admin.rechazo.definitivo')}</p>
          <div className="campo">
            <label htmlFor={`${id}-justificacion`}>{t('admin.rechazo.etiqueta')}</label>
            <textarea
              ref={campo}
              id={`${id}-justificacion`}
              className="dialogo-cuentas__texto"
              rows={5}
              value={texto}
              aria-invalid={errorMostrado ? true : undefined}
              aria-describedby={describe}
              onChange={(evento) => {
                setTexto(evento.target.value)
                setErrorServidor(null)
              }}
            />
            <p
              id={`${id}-contador`}
              className={`dialogo-cuentas__contador${longitud > MAXIMO_JUSTIFICACION ? ' dialogo-cuentas__contador--excedido' : ''}`}
            >
              {t('admin.rechazo.contador', { n: longitud, maximo: MAXIMO_JUSTIFICACION })}
            </p>
            {errorMostrado && (
              <p id={`${id}-error`} className="campo__error" role="alert">
                {t(`admin.rechazo.errores.${errorMostrado}`, {
                  maximo: MAXIMO_JUSTIFICACION,
                  defaultValue: t('admin.rechazo.errores.REQUERIDO'),
                })}
              </p>
            )}
          </div>
          {error && (
            <p className="dialogo__error" role="alert">
              {t(`admin.errores.${error}`, { defaultValue: t('admin.errores.GENERICO') })}
            </p>
          )}
          <div className="dialogo__acciones">
            <button
              type="button"
              className="boton-secundario"
              onClick={() => {
                if (!enviando) onCerrar()
              }}
            >
              {t('admin.dialogo.cancelar')}
            </button>
            <button
              type="submit"
              className={`boton-principal${bloqueado ? ' deshabilitado' : ''}`}
              aria-disabled={bloqueado ? true : undefined}
              aria-busy={enviando ? true : undefined}
            >
              {enviando ? t('admin.dialogo.enviando') : t('admin.rechazo.confirmar')}
            </button>
          </div>
        </form>
      )}
    </DialogoAdmin>
  )
}
