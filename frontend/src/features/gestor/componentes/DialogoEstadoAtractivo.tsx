import { useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError, ERROR_RED, servicioNoDisponible } from '../../../shared/api/httpClient'
import { DialogoAdmin } from '../../admin/componentes/DialogoAdmin'
import type { EstadoOperativo } from '../../catalogo/catalogoApi'
import {
  ESTADOS_OPERATIVOS,
  cambiarEstadoAtractivo,
  consultarAtractivoGestion,
  type AtractivoGestion,
} from '../atractivosGestorApi'

/** Lo que el diálogo necesita del atractivo: la versión leída y el estado actual (DEC-207). */
export interface AtractivoParaEstado {
  id: string
  nombre: string
  estadoOperativo: EstadoOperativo | null
  version: number
}

type Aviso = 'MODIFICADO' | 'MISMO_ESTADO' | 'DATOS_INVALIDOS' | 'SERVICIO_NO_DISPONIBLE' | 'ERROR_RED' | 'GENERICO'

/**
 * CU «Cambiar estado operativo» (RF33; DEC-210, DEC-225), desde el panel (WF-24) y desde la edición. Se elige uno de
 * los otros dos estados y se envía con la versión leída. Con 409 ATRACTIVO_MODIFICADO se relee el atractivo: si lo
 * elegido ya es su estado, se avisa y solo queda cerrar; si no, se conserva lo elegido, se avisa y el reenvío es otro
 * clic con la versión nueva. Con 404, `onNoEncontrado` (quien lo abrió vuelve al panel con un aviso). Un 401 ya cerró la
 * sesión (httpClient).
 */
export function DialogoEstadoAtractivo({
  atractivo,
  onCerrar,
  onCambiado,
  onNoEncontrado,
}: {
  atractivo: AtractivoParaEstado
  onCerrar: () => void
  onCambiado: (actualizado: AtractivoGestion) => void
  onNoEncontrado: () => void
}) {
  const { t } = useTranslation()
  const id = useId()
  const [leido, setLeido] = useState(atractivo)
  const otros = ESTADOS_OPERATIVOS.filter((estado) => estado !== leido.estadoOperativo)
  const [elegido, setElegido] = useState<EstadoOperativo>(otros[0])
  const [aviso, setAviso] = useState<Aviso | null>(null)
  const [enviando, setEnviando] = useState(false)
  const enCurso = useRef(false)
  const primero = useRef<HTMLInputElement>(null)
  const cerrar = useRef<HTMLButtonElement>(null)
  const mismoEstado = elegido === leido.estadoOperativo

  const releer = async () => {
    try {
      const actual = await consultarAtractivoGestion(leido.id)
      setLeido({ id: actual.id, nombre: actual.nombre, estadoOperativo: actual.estadoOperativo, version: actual.version })
      setAviso(actual.estadoOperativo === elegido ? 'MISMO_ESTADO' : 'MODIFICADO')
    } catch (fallo) {
      if (fallo instanceof ApiError && fallo.status === 404) onNoEncontrado()
      else setAviso('GENERICO')
    }
  }

  const confirmar = async () => {
    if (enCurso.current || mismoEstado) return
    enCurso.current = true
    setEnviando(true)
    setAviso(null)
    try {
      onCambiado(await cambiarEstadoAtractivo(leido.id, elegido, leido.version))
    } catch (fallo) {
      if (!(fallo instanceof ApiError)) setAviso('GENERICO')
      else if (fallo.status === 401) return
      else if (fallo.codigo === 'ATRACTIVO_MODIFICADO') await releer()
      else if (fallo.status === 404) onNoEncontrado()
      else if (fallo.codigo === 'DATOS_INVALIDOS') setAviso('DATOS_INVALIDOS')
      else if (servicioNoDisponible(fallo)) setAviso('SERVICIO_NO_DISPONIBLE')
      else if (fallo.codigo === ERROR_RED) setAviso('ERROR_RED')
      else setAviso('GENERICO')
    } finally {
      enCurso.current = false
      setEnviando(false)
    }
  }

  return (
    <DialogoAdmin
      titulo={t('gestor.estado.titulo', { nombre: leido.nombre })}
      bloqueado={enviando}
      focoInicial={primero}
      onCerrar={onCerrar}
    >
      {(idTexto) => (
        <>
          <div id={idTexto}>
            <p>
              {t('gestor.estado.actual', {
                estado: leido.estadoOperativo ? t(`catalogo.estados.${leido.estadoOperativo}`) : '—',
              })}
            </p>
            <fieldset className="dialogo-cuentas__roles">
              <legend>{t('gestor.estado.nuevo')}</legend>
              {ESTADOS_OPERATIVOS.map((estado) => {
                const actual = estado === leido.estadoOperativo
                return (
                  <label key={estado} htmlFor={`${id}-${estado}`}>
                    <input
                      ref={estado === otros[0] ? primero : undefined}
                      id={`${id}-${estado}`}
                      type="radio"
                      name={`${id}-estado`}
                      value={estado}
                      checked={elegido === estado}
                      disabled={actual}
                      onChange={() => {
                        setElegido(estado)
                        if (aviso === 'MISMO_ESTADO') setAviso(null)
                      }}
                    />
                    {t(`catalogo.estados.${estado}`)}
                    {actual && <span className="campo__ayuda"> {t('gestor.estado.esElActual')}</span>}
                  </label>
                )
              })}
            </fieldset>
            <p className="campo__ayuda">{t('gestor.estado.ayuda')}</p>
          </div>
          {aviso && (
            <p className="dialogo__error" role="alert">
              {t(`gestor.estado.avisos.${aviso}`, {
                estado: leido.estadoOperativo ? t(`catalogo.estados.${leido.estadoOperativo}`) : '—',
              })}
            </p>
          )}
          <div className="dialogo__acciones">
            <button ref={cerrar} type="button" className="boton-secundario" disabled={enviando} onClick={onCerrar}>
              {aviso === 'MISMO_ESTADO' ? t('gestor.estado.cerrar') : t('gestor.estado.cancelar')}
            </button>
            <button
              type="button"
              className="boton-principal"
              disabled={enviando || mismoEstado}
              aria-busy={enviando || undefined}
              onClick={() => void confirmar()}
            >
              {enviando ? t('gestor.estado.enviando') : t('gestor.estado.confirmar')}
            </button>
          </div>
        </>
      )}
    </DialogoAdmin>
  )
}
