import { useEffect, useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { ApiError, servicioNoDisponible } from '../../shared/api/httpClient'
import { Paginacion } from '../../shared/components/Paginacion'
import { SinPermiso } from '../../shared/components/SinPermiso'
import { formatearDia } from '../../shared/formato'
import { avisarCambioAvisos, TIPOS_AVISO, type Aviso, type TipoAviso } from '../contenido/contenidoApi'
import type { PaginaCuentas } from './adminApi'
import { DialogoConfirmacion } from './componentes/DialogoConfirmacion'
import { InsigniaCuenta } from './componentes/InsigniaCuenta'
import { TablaCuentas, type ColumnaCuentas, type FilaCuentas } from './componentes/TablaCuentas'
import { despublicarAviso, listarAvisos, publicarAviso } from './contenidoAdminApi'
import { extracto, longitudTexto, MAXIMO_MENSAJE, validarTexto } from './textoAcotado'
import { clasificarError, type ErrorCuentas } from './useCuentas'
import '../contenido/contenido.css'
import './admin.css'

type Estado = { estado: 'cargando' } | { estado: 'ok'; pagina: PaginaCuentas<Aviso> } | { estado: 'error'; error: ErrorCuentas }

interface Envio {
  enviando: boolean
  error: string | null
}

interface EnvioFormulario extends Envio {
  /** Códigos de `campos.mensaje` y `campos.tipo` del último 400. */
  campos: { mensaje?: string; tipo?: string }
}

const SIN_ENVIO: Envio = { enviando: false, error: null }
const FORMULARIO_SIN_ENVIO: EnvioFormulario = { ...SIN_ENVIO, campos: {} }

/** Errores que cambian la lista por debajo (DEC-74, como DEC-52): se avisa, se cierra el diálogo y se recarga. */
const CAMBIAN_LA_LISTA = new Set(['ESTADO_CAMBIADO', 'AVISO_NO_ENCONTRADO'])

/** Máximo de puntos de código del mensaje en el título del diálogo de confirmación. */
const EXTRACTO_DIALOGO = 60

function codigoMostrado(error: ApiError): string {
  if (servicioNoDisponible(error)) return 'CONTENIDO_NO_DISPONIBLE'
  return error.codigo === 'ERROR_RED' ? 'ERROR_RED' : 'GENERICO'
}

/** Página de la URL (desde 1, como WF-12); la API empieza en 0. */
function leerPagina(parametros: URLSearchParams): number {
  const pagina = Number(parametros.get('pagina'))
  return Number.isInteger(pagina) && pagina >= 1 ? pagina : 1
}

/**
 * WF-14, gestión de avisos generales (CU «Crear y publicar aviso general», RF40), solo para el Administrador
 * (RutaProtegida con `rol`, DEC-51, DEC-70). Formulario «Nuevo aviso» en línea (mensaje de 1 a 300 y tipo, «Clima» por
 * defecto, como WF-14) y listado de 10 por página con «Despublicar» en los activos, con confirmación. Los avisos no se
 * editan ni se borran (DEC-64). Un 409 ESTADO_CAMBIADO o un 404 avisan y recargan (DEC-74). Cada cambio avisa al
 * banner para que se actualice sin recargar la página (DEC-72).
 */
export function AdministrarAvisosPage() {
  const { t, i18n } = useTranslation()
  const id = useId()
  const [parametros, setParametros] = useSearchParams()
  const pagina = leerPagina(parametros)
  const [estado, setEstado] = useState<Estado>({ estado: 'cargando' })
  const [intento, setIntento] = useState(0)
  const [anuncio, setAnuncio] = useState('')
  const [sinPermiso, setSinPermiso] = useState(false)

  const [mensaje, setMensaje] = useState('')
  const [tipo, setTipo] = useState<TipoAviso>('Clima')
  const [formulario, setFormulario] = useState<EnvioFormulario>(FORMULARIO_SIN_ENVIO)

  const [aDespublicar, setADespublicar] = useState<Aviso | null>(null)
  const [envio, setEnvio] = useState<Envio>(SIN_ENVIO)

  const tituloResultados = useRef<HTMLHeadingElement>(null)
  const origen = useRef<HTMLElement | null>(null)
  const enfocar = useRef(false)

  useEffect(() => {
    let vigente = true
    setEstado({ estado: 'cargando' })
    listarAvisos(pagina)
      .then((datos) => {
        if (vigente) setEstado({ estado: 'ok', pagina: datos })
      })
      .catch((error: unknown) => {
        if (vigente) setEstado({ estado: 'error', error: clasificarError(error) })
      })
    return () => {
      vigente = false
    }
  }, [pagina, intento])

  useEffect(() => {
    if (!enfocar.current || estado.estado === 'cargando') return
    enfocar.current = false
    tituloResultados.current?.focus()
  })

  if (sinPermiso || (estado.estado === 'error' && estado.error === 'sinPermiso')) return <SinPermiso />

  const recargar = () => {
    enfocar.current = true
    setIntento((n) => n + 1)
  }

  const irAPagina = (numero: number) => {
    setAnuncio('')
    setParametros(numero > 1 ? { pagina: String(numero) } : {})
  }

  /**
   * Atiende el error de una acción del Administrador. Devuelve el código a mostrar junto a la acción, o null si ya se
   * atendió (lista cambiada, sesión cerrada o sin permiso).
   */
  const atenderError = (error: unknown): string | null => {
    if (!(error instanceof ApiError)) return 'GENERICO'
    if (CAMBIAN_LA_LISTA.has(error.codigo)) {
      setADespublicar(null)
      setEnvio(SIN_ENVIO)
      setAnuncio(t(`admin.avisos.errores.${error.codigo}`))
      recargar()
      avisarCambioAvisos()
      return null
    }
    if (error.status === 401) return null // La sesión ya se cerró (httpClient, DEC-50).
    if (clasificarError(error) === 'sinPermiso') {
      setSinPermiso(true)
      return null
    }
    return codigoMostrado(error)
  }

  const invalido = validarTexto(mensaje, MAXIMO_MENSAJE)
  const longitud = longitudTexto(mensaje)
  const bloqueado = formulario.enviando || invalido !== null
  const errorMensaje = formulario.campos.mensaje ?? (invalido === 'LONGITUD_MAXIMA' ? invalido : null)
  const errorTipo = formulario.campos.tipo ?? null

  const publicar = async () => {
    setAnuncio('')
    setFormulario({ enviando: true, error: null, campos: {} })
    try {
      await publicarAviso(mensaje, tipo)
    } catch (error: unknown) {
      if (error instanceof ApiError && error.codigo === 'DATOS_INVALIDOS') {
        const campos = (error.problema?.campos ?? {}) as Record<string, string>
        const conCampo = Boolean(campos.mensaje || campos.tipo)
        setFormulario({ enviando: false, error: conCampo ? null : 'GENERICO', campos: { mensaje: campos.mensaje, tipo: campos.tipo } })
      } else {
        setFormulario({ enviando: false, error: atenderError(error), campos: {} })
      }
      return
    }
    // El nuevo es el más reciente: queda primero en la página 1.
    setFormulario(FORMULARIO_SIN_ENVIO)
    setMensaje('')
    setTipo('Clima')
    setAnuncio(t('admin.avisos.exito.publicado'))
    avisarCambioAvisos()
    enfocar.current = true
    if (pagina === 1) recargar()
    else setParametros({})
  }

  const despublicar = async (aviso: Aviso) => {
    setEnvio({ enviando: true, error: null })
    let actualizado: Aviso
    try {
      actualizado = await despublicarAviso(aviso.id)
    } catch (error: unknown) {
      const codigo = atenderError(error)
      if (codigo) setEnvio({ enviando: false, error: codigo })
      return
    }
    // La respuesta tiene la forma de la fila: se reemplaza en su sitio (DEC-52).
    setEstado((actual) =>
      actual.estado === 'ok'
        ? {
            ...actual,
            pagina: {
              ...actual.pagina,
              elementos: actual.pagina.elementos.map((fila) => (fila.id === actualizado.id ? actualizado : fila)),
            },
          }
        : actual,
    )
    setADespublicar(null)
    setEnvio(SIN_ENVIO)
    setAnuncio(t('admin.avisos.exito.despublicado', { mensaje: extracto(aviso.mensaje, EXTRACTO_DIALOGO) }))
    avisarCambioAvisos()
    // El botón de la fila ya no existe: el foco va al título de resultados.
    tituloResultados.current?.focus()
  }

  const datos = estado.estado === 'ok' ? estado.pagina : null
  const vacia = datos !== null && datos.elementos.length === 0

  const tabla = (() => {
    if (!datos || vacia) return null
    const columnas: ColumnaCuentas[] = ['mensaje', 'tipo', 'publicado', 'estado', 'accion'].map((clave) => ({
      clave,
      titulo: t(`admin.avisos.columnas.${clave}`),
    }))
    const filas: FilaCuentas[] = datos.elementos.map((aviso) => ({
      id: aviso.id,
      celdas: {
        // Recorte solo visual (CSS): el texto completo queda en el DOM para los lectores de pantalla.
        mensaje: <span className="mensaje-aviso">{aviso.mensaje}</span>,
        tipo: <span className={`insignia-aviso insignia-aviso--${aviso.tipo}`}>{t(`avisos.tipos.${aviso.tipo}`)}</span>,
        publicado: formatearDia(aviso.fechaPublicacion, i18n.language),
        estado: <InsigniaCuenta estado={aviso.activo ? 'Activo' : 'Inactivo'} />,
        accion: aviso.activo ? (
          <button
            type="button"
            className="boton-secundario"
            onClick={() => {
              origen.current = document.activeElement instanceof HTMLElement ? document.activeElement : null
              setAnuncio('')
              setEnvio(SIN_ENVIO)
              setADespublicar(aviso)
            }}
          >
            {t('admin.avisos.despublicar')}
            <span className="solo-lector">: {extracto(aviso.mensaje, EXTRACTO_DIALOGO)}</span>
          </button>
        ) : (
          <>
            <span aria-hidden="true">—</span>
            <span className="solo-lector">{t('admin.avisos.sinAccion')}</span>
          </>
        ),
      },
    }))
    return <TablaCuentas titulo={t('admin.avisos.tablaTitulo')} columnas={columnas} filas={filas} />
  })()

  return (
    <div className="admin-cuentas">
      <h1>{t('admin.avisos.titulo')}</h1>
      <p className="admin-cuentas__apoyo">{t('admin.avisos.apoyo')}</p>

      <section className="formulario-aviso" aria-labelledby={`${id}-nuevo`}>
        <h2 id={`${id}-nuevo`}>{t('admin.avisos.nuevo')}</h2>
        <form
          noValidate
          onSubmit={(evento) => {
            evento.preventDefault()
            if (!bloqueado) void publicar()
          }}
        >
          <div className="campo">
            <label htmlFor={`${id}-mensaje`}>{t('admin.avisos.mensaje')}</label>
            <textarea
              id={`${id}-mensaje`}
              className="dialogo-cuentas__texto"
              rows={3}
              value={mensaje}
              aria-invalid={errorMensaje ? true : undefined}
              aria-describedby={[`${id}-contador`, errorMensaje ? `${id}-error-mensaje` : null].filter(Boolean).join(' ')}
              onChange={(evento) => {
                setMensaje(evento.target.value)
                setFormulario((actual) => ({ ...actual, campos: { ...actual.campos, mensaje: undefined } }))
              }}
            />
            <p
              id={`${id}-contador`}
              className={`formulario-aviso__contador${longitud > MAXIMO_MENSAJE ? ' formulario-aviso__contador--excedido' : ''}`}
            >
              {t('admin.contenido.contador', { n: longitud, maximo: MAXIMO_MENSAJE })}
            </p>
            {errorMensaje && (
              <p id={`${id}-error-mensaje`} className="campo__error" role="alert">
                {t(`admin.contenido.errores.${errorMensaje}`, {
                  maximo: MAXIMO_MENSAJE,
                  defaultValue: t('admin.contenido.errores.REQUERIDO'),
                })}
              </p>
            )}
          </div>
          <div className="campo formulario-aviso__tipo">
            <label htmlFor={`${id}-tipo`}>{t('admin.avisos.tipo')}</label>
            <div className="campo__control">
              <select
                id={`${id}-tipo`}
                value={tipo}
                aria-invalid={errorTipo ? true : undefined}
                aria-describedby={errorTipo ? `${id}-error-tipo` : undefined}
                onChange={(evento) => {
                  setTipo(evento.target.value as TipoAviso)
                  setFormulario((actual) => ({ ...actual, campos: { ...actual.campos, tipo: undefined } }))
                }}
              >
                {TIPOS_AVISO.map((valor) => (
                  <option key={valor} value={valor}>
                    {t(`avisos.tipos.${valor}`)}
                  </option>
                ))}
              </select>
            </div>
            {errorTipo && (
              <p id={`${id}-error-tipo`} className="campo__error" role="alert">
                {t(`admin.avisos.erroresTipo.${errorTipo}`, { defaultValue: t('admin.avisos.erroresTipo.REQUERIDO') })}
              </p>
            )}
          </div>
          {formulario.error && (
            <p className="aviso-cuentas aviso-cuentas--error" role="alert">
              {t(`admin.errores.${formulario.error}`, { defaultValue: t('admin.errores.GENERICO') })}
            </p>
          )}
          <button
            type="submit"
            className={`boton-principal${bloqueado ? ' deshabilitado' : ''}`}
            aria-disabled={bloqueado ? true : undefined}
            aria-busy={formulario.enviando ? true : undefined}
          >
            {formulario.enviando ? t('admin.avisos.publicando') : t('admin.avisos.publicar')}
          </button>
        </form>
      </section>

      <section className="admin-cuentas__resultados" aria-labelledby={`${id}-resultados`}>
        <h2 id={`${id}-resultados`} ref={tituloResultados} tabIndex={-1}>
          {datos ? t('admin.avisos.contador', { count: datos.totalElementos }) : t('admin.avisos.publicados')}
        </h2>
        {/* Región de estado de WF-14: publicado, despublicado, 409, 404 y lista vacía. Siempre presente. */}
        <div className="admin-cuentas__region" role="status" aria-live="polite">
          {anuncio && <p className="aviso-cuentas">{anuncio}</p>}
          {vacia && datos && (
            <p className="aviso-cuentas">
              {t(datos.totalElementos > 0 ? 'admin.avisos.fueraDeRango' : 'admin.avisos.vacia')}
            </p>
          )}
        </div>

        {estado.estado === 'cargando' && (
          <p className="estado-carga" role="status" aria-busy="true">
            {t('admin.avisos.cargando')}
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
        {datos && datos.totalPaginas > 1 && (
          <div className="admin-cuentas__paginacion">
            <p>{t('admin.paginaDe', { actual: pagina, total: datos.totalPaginas })}</p>
            <Paginacion actual={pagina} total={datos.totalPaginas} onCambiar={irAPagina} />
          </div>
        )}
      </section>

      {aDespublicar && (
        <DialogoConfirmacion
          accion="despublicar"
          nombre={extracto(aDespublicar.mensaje, EXTRACTO_DIALOGO)}
          enviando={envio.enviando}
          error={envio.error}
          onConfirmar={() => void despublicar(aDespublicar)}
          onCerrar={() => {
            setADespublicar(null)
            setEnvio(SIN_ENVIO)
            origen.current?.focus()
          }}
        />
      )}
    </div>
  )
}
