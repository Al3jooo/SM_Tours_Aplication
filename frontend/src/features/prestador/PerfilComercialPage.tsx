import { useId, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError, ERROR_RED, servicioNoDisponible } from '../../shared/api/httpClient'
import { longitudTexto } from '../admin/textoAcotado'
import { Campo } from '../auth/registro/Campo'
import { enfocarPrimerError, textoErrorCampo } from '../auth/registro/envioRegistro'
import {
  LONGITUD_MAXIMA,
  erroresDelServidor,
  normalizarCorreo,
  type ErroresCampos,
} from '../auth/registro/validacionRegistro'
import { actualizarPerfilPrestador, type CambiosPerfilPrestador } from './perfilPrestadorApi'
import { usePerfilPrestador } from './usePerfilPrestador'
import './prestador.css'

const CAMPOS = ['correoCorporativo', 'telefono'] as const
const ID = (campo: string) => `perfil-prestador-${campo}`
/** Misma forma que el registro (RF02). */
const FORMA_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Validación mínima en el cliente (DEC-166), con las reglas del contrato: obligatorios, longitudes y forma del correo. */
export function validarPerfilComercial(cambios: CambiosPerfilPrestador): ErroresCampos {
  const errores: ErroresCampos = {}
  const correo = normalizarCorreo(cambios.correoCorporativo)
  if (!correo) errores.correoCorporativo = 'REQUERIDO'
  else if (longitudTexto(correo) > LONGITUD_MAXIMA.correoCorporativo) errores.correoCorporativo = 'LONGITUD_MAXIMA'
  else if (!FORMA_CORREO.test(correo)) errores.correoCorporativo = 'FORMATO'
  const telefono = cambios.telefono.trim()
  if (!telefono) errores.telefono = 'REQUERIDO'
  else if (longitudTexto(telefono) > LONGITUD_MAXIMA.telefono) errores.telefono = 'LONGITUD_MAXIMA'
  return errores
}

function aRechazo(error: unknown): { campos: ErroresCampos; general: string | null } {
  if (!(error instanceof ApiError)) return { campos: {}, general: 'GENERICO' }
  if (error.codigo === 'DATOS_INVALIDOS') {
    const todos = erroresDelServidor(error.problema?.campos)
    const propios = Object.fromEntries(Object.entries(todos).filter(([campo]) => (CAMPOS as readonly string[]).includes(campo)))
    const completos = Object.keys(propios).length > 0 && Object.keys(propios).length === Object.keys(todos).length
    return { campos: propios, general: completos ? null : 'DATOS_INVALIDOS' }
  }
  if (servicioNoDisponible(error)) return { campos: {}, general: 'SERVICIO_NO_DISPONIBLE' }
  if (error.codigo === ERROR_RED) return { campos: {}, general: 'ERROR_RED' }
  return { campos: {}, general: 'GENERICO' }
}

/**
 * WF-22 «Mi perfil comercial» (RF43, CU «Actualizar datos comerciales», flujo A; DEC-196, DEC-202), solo para el
 * Prestador. Muestra en solo lectura el correo de acceso, el representante legal, la razón social y el RNT (O16) y
 * edita el correo corporativo y el teléfono. El correo de acceso es la credencial y no se edita (ADR-006, decisión D2):
 * una nota lo distingue del correo corporativo y del contacto de cada servicio (C2). El PUT envía solo esos dos campos.
 */
export function PerfilComercialPage() {
  const { t } = useTranslation()
  const id = useId()
  const { carga, recargar, reemplazar } = usePerfilPrestador()
  /** Lo escrito; null mientras no se cambió nada (entonces se muestran los valores guardados). */
  const [escrito, setEscrito] = useState<CambiosPerfilPrestador | null>(null)
  const [errores, setErrores] = useState<ErroresCampos>({})
  const [estado, setEstado] = useState<{ clave: string; error: boolean } | null>(null)
  const [enviando, setEnviando] = useState(false)
  const enCurso = useRef(false)
  const guardado = carga.estado === 'ok' ? carga.perfil : null
  const cambios = escrito ?? (guardado ? { correoCorporativo: guardado.correoCorporativo, telefono: guardado.telefono } : null)

  if (carga.estado === 'error') {
    return (
      <div className="prestador">
        <h1>{t('prestador.perfil.titulo')}</h1>
        <div className="prestador__error-general" role="alert">
          <p>{t('prestador.perfil.errores.carga')}</p>
          <button type="button" className="boton-secundario" onClick={recargar}>
            {t('catalogo.busqueda.reintentar')}
          </button>
        </div>
      </div>
    )
  }
  if (!guardado || !cambios) {
    return (
      <div className="prestador">
        <h1>{t('prestador.perfil.titulo')}</h1>
        <p className="estado-carga" role="status" aria-busy="true">
          {t('prestador.perfil.cargando')}
        </p>
      </div>
    )
  }

  const guardar = async (evento: FormEvent) => {
    evento.preventDefault()
    if (enCurso.current) return
    setEstado(null)
    const locales = validarPerfilComercial(cambios)
    setErrores(locales)
    if (Object.keys(locales).length > 0) {
      enfocarPrimerError(CAMPOS, locales, ID)
      return
    }
    enCurso.current = true
    setEnviando(true)
    try {
      const resultado = await actualizarPerfilPrestador(cambios)
      reemplazar(resultado)
      setEscrito(null)
      setEstado({ clave: 'prestador.perfil.guardado', error: false })
    } catch (fallo) {
      // Un 401 ya cerró la sesión (httpClient) y RutaProtegida lleva al login.
      if (fallo instanceof ApiError && fallo.status === 401) return
      if (fallo instanceof ApiError && fallo.status === 403) {
        setEstado({ clave: 'prestador.perfil.errores.ACCESO_DENEGADO', error: true })
        return
      }
      const rechazo = aRechazo(fallo)
      setErrores(rechazo.campos)
      if (rechazo.general) setEstado({ clave: `prestador.perfil.errores.${rechazo.general}`, error: true })
      enfocarPrimerError(CAMPOS, rechazo.campos, ID)
    } finally {
      enCurso.current = false
      setEnviando(false)
    }
  }

  const descartar = () => {
    setEscrito(null)
    setErrores({})
    setEstado(null)
  }
  const cambiar = (campo: keyof CambiosPerfilPrestador) => (valor: string) => {
    setEscrito((actual) => ({ ...(actual ?? cambios), [campo]: valor }))
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }
  const error = (campo: string) => textoErrorCampo(t, campo, errores[campo])

  return (
    <div className="prestador">
      <h1>{t('prestador.perfil.titulo')}</h1>
      <p className="prestador__apoyo">{t('prestador.perfil.apoyo')}</p>

      <section className="tarjeta prestador__seccion" aria-labelledby={`${id}-registro`}>
        <h2 id={`${id}-registro`}>{t('prestador.perfil.registro')}</h2>
        <p className="campo__ayuda">{t('prestador.perfil.soloLectura')}</p>
        <dl className="prestador__datos">
          <div>
            <dt>{t('prestador.perfil.correoAcceso')}</dt>
            <dd>{guardado.correo}</dd>
          </div>
          <div>
            <dt>{t('prestador.perfil.representanteLegal')}</dt>
            <dd>{guardado.representanteLegal}</dd>
          </div>
          <div>
            <dt>{t('prestador.perfil.razonSocial')}</dt>
            <dd>
              {guardado.razonSocial ?? (
                <>
                  <span aria-hidden="true">—</span>
                  <span className="solo-lector">{t('prestador.perfil.sinDato')}</span>
                </>
              )}
            </dd>
          </div>
          <div>
            <dt>{t('prestador.campos.rnt')}</dt>
            <dd>{guardado.rnt}</dd>
          </div>
        </dl>
        <p className="prestador__nota">{t('prestador.perfil.notaCorreo')}</p>
      </section>

      <form className="tarjeta prestador__seccion" noValidate onSubmit={(evento) => void guardar(evento)} aria-labelledby={`${id}-contacto`}>
        <h2 id={`${id}-contacto`}>{t('prestador.perfil.contacto')}</h2>
        <div className="prestador__columnas">
          <Campo id={ID('correoCorporativo')} etiqueta={t('prestador.perfil.correoCorporativo')} type="email"
            autoComplete="email" ayuda={t('prestador.perfil.correoCorporativoAyuda')} value={cambios.correoCorporativo}
            onChange={(evento) => cambiar('correoCorporativo')(evento.target.value)} error={error('correoCorporativo')} />
          <Campo id={ID('telefono')} etiqueta={t('prestador.perfil.telefono')} type="tel" autoComplete="tel"
            value={cambios.telefono} onChange={(evento) => cambiar('telefono')(evento.target.value)}
            error={error('telefono')} />
        </div>
        <div className="prestador__acciones">
          <button type="submit" className="boton-principal" disabled={enviando} aria-busy={enviando || undefined}>
            {enviando ? t('prestador.edicion.guardando') : t('prestador.edicion.guardar')}
          </button>
          <button type="button" className="boton-secundario" onClick={descartar} disabled={enviando}>
            {t('prestador.edicion.descartar')}
          </button>
        </div>
        <p
          className={estado ? (estado.error ? 'prestador__error-general' : 'prestador__aviso prestador__aviso--exito') : undefined}
          role="status"
        >
          {estado && t(estado.clave)}
        </p>
      </form>
    </div>
  )
}
