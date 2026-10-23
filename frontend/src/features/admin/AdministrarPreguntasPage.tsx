import { useEffect, useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError, servicioNoDisponible } from '../../shared/api/httpClient'
import { SinPermiso } from '../../shared/components/SinPermiso'
import { formatearDia } from '../../shared/formato'
import { listarPreguntas, type PreguntaFrecuente } from '../contenido/contenidoApi'
import { DialogoConfirmacion } from './componentes/DialogoConfirmacion'
import { DialogoPregunta, type ErroresPregunta } from './componentes/DialogoPregunta'
import { TablaCuentas, type ColumnaCuentas, type FilaCuentas } from './componentes/TablaCuentas'
import { crearPregunta, editarPregunta, eliminarPregunta, type DatosPregunta } from './contenidoAdminApi'
import { extracto } from './textoAcotado'
import { clasificarError, type ErrorCuentas } from './useCuentas'
import './admin.css'

/** Tope de preguntas (DEC-62): la 201 es 409 LIMITE_FAQ. */
export const MAXIMO_PREGUNTAS = 200

type Estado = { estado: 'cargando' } | { estado: 'ok'; preguntas: PreguntaFrecuente[] } | { estado: 'error'; error: ErrorCuentas }

type Dialogo = { tipo: 'pregunta'; pregunta: PreguntaFrecuente | null } | { tipo: 'eliminar'; pregunta: PreguntaFrecuente }

interface Envio {
  enviando: boolean
  error: string | null
  campos: ErroresPregunta
}

const SIN_ENVIO: Envio = { enviando: false, error: null, campos: {} }

/** Errores que cambian la lista por debajo (DEC-73): se avisa, se cierra el diálogo y se recarga. */
const CAMBIAN_LA_LISTA = new Set(['FAQ_NO_ENCONTRADA', 'LIMITE_FAQ'])

/** Código que se muestra para un error que no es de la lista ni de la sesión. */
function codigoMostrado(error: ApiError): string {
  if (servicioNoDisponible(error)) return 'CONTENIDO_NO_DISPONIBLE'
  return error.codigo === 'ERROR_RED' ? 'ERROR_RED' : 'GENERICO'
}

/**
 * WF-13, gestión de preguntas frecuentes (CU «Crear / Editar / Eliminar pregunta frecuente», RF26), solo para el
 * Administrador (RutaProtegida con `rol`, DEC-51, DEC-70). La lista es la pública, en el orden del servidor (DEC-63):
 * después de crear, editar o eliminar se recarga. Un 404 o un 409 LIMITE_FAQ avisan en la región `aria-live` y recargan
 * (DEC-73). Con 200 preguntas, «Nueva pregunta» queda deshabilitada. La edición no tiene control de concurrencia (L2).
 */
export function AdministrarPreguntasPage() {
  const { t, i18n } = useTranslation()
  const id = useId()
  const [estado, setEstado] = useState<Estado>({ estado: 'cargando' })
  const [intento, setIntento] = useState(0)
  const [anuncio, setAnuncio] = useState('')
  const [sinPermiso, setSinPermiso] = useState(false)
  const [dialogo, setDialogo] = useState<Dialogo | null>(null)
  const [envio, setEnvio] = useState<Envio>(SIN_ENVIO)
  const tituloResultados = useRef<HTMLHeadingElement>(null)
  const origen = useRef<HTMLElement | null>(null)
  /** Después de un cambio, el foco va al título de resultados cuando llega la lista nueva. */
  const enfocar = useRef(false)

  useEffect(() => {
    let vigente = true
    setEstado({ estado: 'cargando' })
    listarPreguntas()
      .then((preguntas) => {
        if (vigente) setEstado({ estado: 'ok', preguntas })
      })
      .catch((error: unknown) => {
        if (vigente) setEstado({ estado: 'error', error: clasificarError(error) })
      })
    return () => {
      vigente = false
    }
  }, [intento])

  useEffect(() => {
    if (!enfocar.current || estado.estado === 'cargando') return
    enfocar.current = false
    tituloResultados.current?.focus()
  })

  if (sinPermiso) return <SinPermiso />

  const recargar = () => {
    enfocar.current = true
    setIntento((n) => n + 1)
  }

  const abrirDialogo = (nuevo: Dialogo) => {
    origen.current = document.activeElement instanceof HTMLElement ? document.activeElement : null
    setEnvio(SIN_ENVIO)
    setAnuncio('')
    setDialogo(nuevo)
  }

  const cerrarDialogo = () => {
    setDialogo(null)
    setEnvio(SIN_ENVIO)
    origen.current?.focus()
  }

  /** Éxito, 404 o 409: se cierra el diálogo, se avisa y se recarga la lista. */
  const terminar = (texto: string) => {
    setDialogo(null)
    setEnvio(SIN_ENVIO)
    setAnuncio(texto)
    recargar()
  }

  /** Ejecuta una acción del Administrador y atiende sus errores; devuelve false si falló. */
  const ejecutar = async (accion: () => Promise<unknown>): Promise<boolean> => {
    setEnvio({ enviando: true, error: null, campos: {} })
    try {
      await accion()
      return true
    } catch (error: unknown) {
      if (!(error instanceof ApiError)) {
        setEnvio({ enviando: false, error: 'GENERICO', campos: {} })
      } else if (CAMBIAN_LA_LISTA.has(error.codigo)) {
        terminar(t(`admin.preguntas.errores.${error.codigo}`))
      } else if (error.status === 401) {
        // La sesión ya se cerró (httpClient); RutaProtegida lleva al login con el aviso (DEC-50).
        setEnvio(SIN_ENVIO)
      } else if (clasificarError(error) === 'sinPermiso') {
        setSinPermiso(true)
      } else if (error.codigo === 'DATOS_INVALIDOS') {
        const campos = (error.problema?.campos ?? {}) as Record<string, string>
        const errores: ErroresPregunta = { titulo: campos.titulo, cuerpo: campos.cuerpo }
        const conCampo = Boolean(errores.titulo || errores.cuerpo)
        setEnvio({ enviando: false, error: conCampo ? null : 'GENERICO', campos: errores })
      } else {
        setEnvio({ enviando: false, error: codigoMostrado(error), campos: {} })
      }
      return false
    }
  }

  const guardar = async (pregunta: PreguntaFrecuente | null, datos: DatosPregunta) => {
    const exito = await ejecutar(() => (pregunta ? editarPregunta(pregunta.id, datos) : crearPregunta(datos)))
    if (exito) {
      terminar(t(pregunta ? 'admin.preguntas.exito.editada' : 'admin.preguntas.exito.creada', { titulo: datos.titulo.trim() }))
    }
  }

  const eliminar = async (pregunta: PreguntaFrecuente) => {
    const exito = await ejecutar(() => eliminarPregunta(pregunta.id))
    if (exito) terminar(t('admin.preguntas.exito.eliminada', { titulo: pregunta.titulo }))
  }

  const preguntas = estado.estado === 'ok' ? estado.preguntas : null
  const lleno = preguntas !== null && preguntas.length >= MAXIMO_PREGUNTAS

  const tabla = (() => {
    if (!preguntas || preguntas.length === 0) return null
    const columnas: ColumnaCuentas[] = ['titulo', 'actualizada', 'acciones'].map((clave) => ({
      clave,
      titulo: t(`admin.preguntas.columnas.${clave}`),
    }))
    const filas: FilaCuentas[] = preguntas.map((pregunta) => ({
      id: pregunta.id,
      celdas: {
        titulo: pregunta.titulo,
        actualizada: formatearDia(pregunta.fechaActualizacion, i18n.language),
        acciones: (
          <div className="acciones-contenido">
            <button type="button" className="boton-secundario" onClick={() => abrirDialogo({ tipo: 'pregunta', pregunta })}>
              {t('admin.preguntas.editar')}
              <span className="solo-lector">: {pregunta.titulo}</span>
            </button>
            <button type="button" className="boton-secundario" onClick={() => abrirDialogo({ tipo: 'eliminar', pregunta })}>
              {t('admin.preguntas.eliminar')}
              <span className="solo-lector">: {pregunta.titulo}</span>
            </button>
          </div>
        ),
      },
    }))
    return <TablaCuentas titulo={t('admin.preguntas.tablaTitulo')} columnas={columnas} filas={filas} />
  })()

  return (
    <div className="admin-cuentas">
      <h1>{t('admin.preguntas.titulo')}</h1>
      <p className="admin-cuentas__apoyo">{t('admin.preguntas.apoyo')}</p>

      <section className="admin-cuentas__resultados" aria-labelledby={`${id}-resultados`}>
        <div className="encabezado-contenido">
          <h2 id={`${id}-resultados`} ref={tituloResultados} tabIndex={-1}>
            {preguntas ? t('admin.preguntas.contador', { n: preguntas.length, maximo: MAXIMO_PREGUNTAS }) : t('admin.preguntas.resultados')}
          </h2>
          <button
            type="button"
            className={`boton-principal${lleno || !preguntas ? ' deshabilitado' : ''}`}
            aria-disabled={lleno || !preguntas ? true : undefined}
            aria-describedby={lleno ? `${id}-limite` : undefined}
            onClick={() => {
              if (preguntas && !lleno) abrirDialogo({ tipo: 'pregunta', pregunta: null })
            }}
          >
            {t('admin.preguntas.nueva')}
          </button>
        </div>
        {lleno && (
          <p id={`${id}-limite`} className="admin-cuentas__apoyo">
            {t('admin.preguntas.limite', { maximo: MAXIMO_PREGUNTAS })}
          </p>
        )}

        {/* Región de estado de WF-13: creada, editada, eliminada, 404, 409 LIMITE_FAQ y lista vacía. Siempre presente. */}
        <div className="admin-cuentas__region" role="status" aria-live="polite">
          {anuncio && <p className="aviso-cuentas">{anuncio}</p>}
          {preguntas !== null && preguntas.length === 0 && <p className="aviso-cuentas">{t('admin.preguntas.vacia')}</p>}
        </div>

        {estado.estado === 'cargando' && (
          <p className="estado-carga" role="status" aria-busy="true">
            {t('admin.preguntas.cargando')}
          </p>
        )}
        {estado.estado === 'error' && (
          <div className="aviso-cuentas aviso-cuentas--error" role="alert">
            <p>{t(`admin.contenido.erroresLista.${estado.error}`)}</p>
            <button type="button" className="boton-secundario" onClick={recargar}>
              {t('catalogo.busqueda.reintentar')}
            </button>
          </div>
        )}
        {tabla}
      </section>

      {dialogo?.tipo === 'pregunta' && (
        <DialogoPregunta
          inicial={dialogo.pregunta && { titulo: dialogo.pregunta.titulo, cuerpo: dialogo.pregunta.cuerpo }}
          enviando={envio.enviando}
          errores={envio.campos}
          error={envio.error}
          onGuardar={(datos) => void guardar(dialogo.pregunta, datos)}
          onCerrar={cerrarDialogo}
        />
      )}
      {dialogo?.tipo === 'eliminar' && (
        <DialogoConfirmacion
          accion="eliminarPregunta"
          nombre={extracto(dialogo.pregunta.titulo, 60)}
          enviando={envio.enviando}
          error={envio.error}
          onConfirmar={() => void eliminar(dialogo.pregunta)}
          onCerrar={cerrarDialogo}
        />
      )}
    </div>
  )
}
