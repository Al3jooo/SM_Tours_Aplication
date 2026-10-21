import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError } from '../../../shared/api/httpClient'
import { consultarPrestador } from '../adminApi'
import { DialogoAdmin } from './DialogoAdmin'

type Motivo = { estado: 'cargando' } | { estado: 'ok'; motivo: string | null } | { estado: 'error'; codigo: string }

/** «Ver motivo» (RF28): solo lectura; el motivo no viene en el listado, se consulta el detalle del prestador. */
export function DialogoMotivo({
  idUsuario,
  nombre,
  onCerrar,
}: {
  idUsuario: string
  nombre: string
  onCerrar: () => void
}) {
  const { t } = useTranslation()
  const cerrar = useRef<HTMLButtonElement>(null)
  const [motivo, setMotivo] = useState<Motivo>({ estado: 'cargando' })
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vigente = true
    setMotivo({ estado: 'cargando' })
    consultarPrestador(idUsuario)
      .then((detalle) => {
        if (vigente) setMotivo({ estado: 'ok', motivo: detalle.motivoRechazo })
      })
      .catch((error: unknown) => {
        if (vigente) setMotivo({ estado: 'error', codigo: error instanceof ApiError ? error.codigo : 'GENERICO' })
      })
    return () => {
      vigente = false
    }
  }, [idUsuario, intento])

  return (
    <DialogoAdmin titulo={t('admin.motivo.titulo', { nombre })} focoInicial={cerrar} onCerrar={onCerrar}>
      {(idTexto) => (
        <>
          <div id={idTexto} aria-live="polite">
            {motivo.estado === 'cargando' && (
              <p className="estado-carga" role="status" aria-busy="true">
                {t('comun.cargando')}
              </p>
            )}
            {motivo.estado === 'ok' && (
              <p className="dialogo-cuentas__motivo">{motivo.motivo ?? t('admin.motivo.sinMotivo')}</p>
            )}
            {motivo.estado === 'error' && (
              <p className="dialogo__error" role="alert">
                {t(`admin.errores.${motivo.codigo}`, { defaultValue: t('admin.errores.GENERICO') })}
              </p>
            )}
          </div>
          <div className="dialogo__acciones">
            {motivo.estado === 'error' && motivo.codigo !== 'CUENTA_NO_ENCONTRADA' && (
              <button type="button" className="boton-secundario" onClick={() => setIntento((n) => n + 1)}>
                {t('catalogo.busqueda.reintentar')}
              </button>
            )}
            <button ref={cerrar} type="button" className="boton-secundario" onClick={onCerrar}>
              {t('admin.dialogo.cerrar')}
            </button>
          </div>
        </>
      )}
    </DialogoAdmin>
  )
}
