import { useEffect, useId, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import type { Reserva } from '../reservasApi'

/** Errores después de los cuales ya no tiene sentido volver a intentar la cancelación. */
const ERRORES_FINALES = new Set(['CANCELACION_FUERA_DE_PLAZO', 'RESERVA_NO_ENCONTRADA'])

/**
 * Confirmación de «Cancelar reserva» (RF18) con <dialog> nativo y showModal() (ajuste 5): el navegador atrapa el foco
 * dentro del diálogo y lo cierra con Esc. Al abrirse, el foco va a «No, volver» (la opción que no destruye nada).
 * Al cerrarse, `onCerrar` avisa a la página, que devuelve el foco al botón que lo abrió.
 */
export function DialogoCancelar({
  reserva,
  enviando,
  error,
  onConfirmar,
  onCerrar,
}: {
  reserva: Reserva
  enviando: boolean
  /** Código del último intento fallido, si lo hubo. */
  error: string | null
  onConfirmar: () => void
  onCerrar: () => void
}) {
  const { t } = useTranslation()
  const id = useId()
  const dialogo = useRef<HTMLDialogElement>(null)
  const volver = useRef<HTMLButtonElement>(null)
  const final = error !== null && ERRORES_FINALES.has(error)

  useEffect(() => {
    const elemento = dialogo.current
    if (elemento && !elemento.open) elemento.showModal()
    volver.current?.focus()
  }, [])

  return (
    <dialog
      ref={dialogo}
      className="dialogo"
      aria-labelledby={`${id}-titulo`}
      aria-describedby={`${id}-texto`}
      onClose={onCerrar}
      onCancel={(evento) => {
        // Mientras se envía la cancelación, Esc no cierra: el resultado debe verse.
        if (enviando) evento.preventDefault()
      }}
    >
      <h2 id={`${id}-titulo`}>{t('misReservas.dialogo.titulo')}</h2>
      <p id={`${id}-texto`}>{t('misReservas.dialogo.texto', { servicio: reserva.nombreServicio })}</p>
      {error && (
        <p className="dialogo__error" role="alert">
          {t(`misReservas.errores.${error}`, { defaultValue: t('misReservas.errores.GENERICO') })}
        </p>
      )}
      <div className="dialogo__acciones">
        {!final && (
          <button
            type="button"
            className={`boton-principal${enviando ? ' deshabilitado' : ''}`}
            aria-disabled={enviando ? true : undefined}
            aria-busy={enviando ? true : undefined}
            onClick={() => {
              if (!enviando) onConfirmar()
            }}
          >
            {enviando ? t('misReservas.dialogo.cancelando') : t('misReservas.dialogo.confirmar')}
          </button>
        )}
        <button
          ref={volver}
          type="button"
          className="boton-secundario"
          onClick={() => {
            if (!enviando) dialogo.current?.close()
          }}
        >
          {final ? t('misReservas.dialogo.cerrar') : t('misReservas.dialogo.volver')}
        </button>
      </div>
    </dialog>
  )
}
