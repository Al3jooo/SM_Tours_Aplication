import { useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { DialogoAdmin } from './DialogoAdmin'

export type AccionConfirmable = 'inactivar' | 'desactivar' | 'reactivar' | 'despublicar' | 'eliminarPregunta'

/**
 * Confirmación de Inactivar (RF22), Desactivar y Reactivar (RF62), sin motivo (DEC-38). Inactivar avisa que no se
 * puede revertir desde el sistema (DEC-37) y que los servicios dejan de publicarse (RF22). También Despublicar un aviso
 * (RF40, DEC-74) y Eliminar una pregunta frecuente (RF26, DEC-73); `nombre` es el extracto del mensaje o el título. El
 * foco inicial va a «Cancelar», la opción que no cambia nada.
 */
export function DialogoConfirmacion({
  accion,
  nombre,
  enviando,
  error,
  onConfirmar,
  onCerrar,
}: {
  accion: AccionConfirmable
  nombre: string
  enviando: boolean
  error: string | null
  onConfirmar: () => void
  onCerrar: () => void
}) {
  const { t } = useTranslation()
  const cancelar = useRef<HTMLButtonElement>(null)

  return (
    <DialogoAdmin
      titulo={t(`admin.confirmacion.${accion}.titulo`, { nombre })}
      bloqueado={enviando}
      focoInicial={cancelar}
      onCerrar={onCerrar}
    >
      {(idTexto) => (
        <>
          <p id={idTexto}>{t(`admin.confirmacion.${accion}.texto`, { nombre })}</p>
          {error && (
            <p className="dialogo__error" role="alert">
              {t(`admin.errores.${error}`, { defaultValue: t('admin.errores.GENERICO') })}
            </p>
          )}
          <div className="dialogo__acciones">
            <button
              ref={cancelar}
              type="button"
              className="boton-secundario"
              onClick={() => {
                if (!enviando) onCerrar()
              }}
            >
              {t('admin.dialogo.cancelar')}
            </button>
            <button
              type="button"
              className={`boton-principal${enviando ? ' deshabilitado' : ''}`}
              aria-disabled={enviando ? true : undefined}
              aria-busy={enviando ? true : undefined}
              onClick={() => {
                if (!enviando) onConfirmar()
              }}
            >
              {enviando ? t('admin.dialogo.enviando') : t(`admin.confirmacion.${accion}.confirmar`)}
            </button>
          </div>
        </>
      )}
    </DialogoAdmin>
  )
}
