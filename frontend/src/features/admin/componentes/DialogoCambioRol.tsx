import { useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ROLES_PERSONAL, type RolCambio, type RolPersonal } from '../adminApi'
import { DialogoAdmin } from './DialogoAdmin'

/** Asignar (Turista → Gestor o Administrador), modificar (Gestor ↔ Administrador) o revocar (→ Turista). */
export type ModoCambioRol = 'asignar' | 'modificar' | 'revocar'

/**
 * Confirmación del cambio de rol (RF45; WF-12, DEC-118). Muestra el rol actual y el nuevo; al asignar, el rol nuevo se
 * elige con radios (Gestor por defecto, el de menos privilegios) y se advierte que la cuenta no usará sus reservas,
 * favoritos ni historial mientras tenga el rol (DEC-112). Al modificar no se advierte: la cuenta ya perdió ese acceso
 * con su primer rol. Al revocar, recupera sus datos de turista. Siempre se avisa que sus sesiones se cerrarán. El foco
 * inicial va al rol elegido (asignar) o a «Cancelar» (modificar y revocar), la opción que no cambia nada.
 */
export function DialogoCambioRol({
  modo,
  nombre,
  rolActual,
  rolNuevo,
  enviando,
  error,
  onConfirmar,
  onCerrar,
}: {
  modo: ModoCambioRol
  /** El nombre de la cuenta o, si no tiene, su correo. */
  nombre: string
  rolActual: RolCambio
  /** Fijo al modificar o revocar; al asignar lo elige el Administrador. */
  rolNuevo?: RolCambio
  enviando: boolean
  error: string | null
  onConfirmar: (rolNuevo: RolCambio) => void
  onCerrar: () => void
}) {
  const { t } = useTranslation()
  const id = useId()
  const cancelar = useRef<HTMLButtonElement>(null)
  const primerRol = useRef<HTMLInputElement>(null)
  const [elegido, setElegido] = useState<RolPersonal>('Gestor')
  const destino: RolCambio = modo === 'asignar' ? elegido : (rolNuevo ?? 'Turista')
  const nombreRol = (rol: RolCambio) => t(`admin.roles.${rol}`)

  return (
    <DialogoAdmin
      titulo={t(`admin.cambioRol.titulo.${modo}`, { nombre })}
      bloqueado={enviando}
      focoInicial={modo === 'asignar' ? primerRol : cancelar}
      onCerrar={onCerrar}
    >
      {(idTexto) => (
        <>
          <div id={idTexto}>
            <p>{t('admin.cambioRol.rolActual', { rol: nombreRol(rolActual) })}</p>
            {modo === 'asignar' ? (
              <fieldset className="dialogo-cuentas__roles">
                <legend>{t('admin.cambioRol.rolNuevo')}</legend>
                {ROLES_PERSONAL.map((rol, indice) => (
                  <label key={rol} htmlFor={`${id}-${rol}`}>
                    <input
                      ref={indice === 0 ? primerRol : undefined}
                      id={`${id}-${rol}`}
                      type="radio"
                      name={`${id}-rol`}
                      value={rol}
                      checked={elegido === rol}
                      onChange={() => setElegido(rol)}
                    />
                    {nombreRol(rol)}
                  </label>
                ))}
              </fieldset>
            ) : (
              <p>{t('admin.cambioRol.rolNuevoFijo', { rol: nombreRol(destino) })}</p>
            )}
            {modo === 'asignar' && <p className="dialogo-cuentas__advertencia">{t('admin.cambioRol.advertencia')}</p>}
            {modo === 'revocar' && <p>{t('admin.cambioRol.revocar', { rol: nombreRol(rolActual) })}</p>}
            <p>{t('admin.cambioRol.sesiones')}</p>
          </div>
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
                if (!enviando) onConfirmar(destino)
              }}
            >
              {enviando
                ? t('admin.dialogo.enviando')
                : t(`admin.cambioRol.confirmar.${modo}`, { rol: nombreRol(destino) })}
            </button>
          </div>
        </>
      )}
    </DialogoAdmin>
  )
}
