import { useCallback, useEffect, useId, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiError, ERROR_RED, servicioNoDisponible } from '../../shared/api/httpClient'
import { useSesion } from '../auth/useSesion'
import { Campo } from '../auth/registro/Campo'
import { CasillasCategorias, idCasillaCategoria } from '../auth/registro/CasillasCategorias'
import { enfocarPrimerError, textoErrorCampo } from '../auth/registro/envioRegistro'
import { SelectorPais } from '../auth/registro/SelectorPais'
import { usePaises } from '../auth/registro/usePaises'
import {
  CATEGORIAS_INTERES,
  erroresDelServidor,
  validarPerfil,
  type DatosPerfil,
  type ErroresCampos,
} from '../auth/registro/validacionRegistro'
import { actualizarPerfil, consultarPerfil, type PerfilTurista } from './perfilApi'
import './perfil.css'

const SECCIONES = ['datos', 'intereses'] as const
type Seccion = (typeof SECCIONES)[number]

const CAMPOS_DATOS = ['primerNombre', 'segundoNombre', 'primerApellido', 'segundoApellido', 'paisOrigen'] as const
type DatosPersonales = Pick<DatosPerfil, (typeof CAMPOS_DATOS)[number]>

const PREFIJO = 'perfil'
const ID = (campo: string) => `${PREFIJO}-${campo}`
const ID_FOCO = (campo: string) =>
  campo === 'categoriasInteres' ? idCasillaCategoria(PREFIJO, CATEGORIAS_INTERES[0]) : ID(campo)

type Carga = { estado: 'cargando' } | { estado: 'error' } | { estado: 'ok'; guardado: PerfilTurista }

/** Región de estado: la confirmación o el error del último guardado (clave i18n). */
interface Estado {
  clave: string
  error: boolean
}

const datosDe = (perfil: PerfilTurista): DatosPersonales => ({
  primerNombre: perfil.primerNombre,
  segundoNombre: perfil.segundoNombre ?? '',
  primerApellido: perfil.primerApellido,
  segundoApellido: perfil.segundoApellido ?? '',
  paisOrigen: perfil.paisOrigen,
})

/** Un rechazo del PUT: errores por campo de la sección o un mensaje general (clave i18n). */
function aRechazoPerfil(error: unknown, campos: readonly string[]): { campos: ErroresCampos; general: string | null } {
  if (!(error instanceof ApiError)) return { campos: {}, general: 'perfil.errores.GENERICO' }
  if (servicioNoDisponible(error)) return { campos: {}, general: 'perfil.errores.SERVICIO_NO_DISPONIBLE' }
  if (error.codigo === ERROR_RED) return { campos: {}, general: 'perfil.errores.ERROR_RED' }
  if (error.codigo === 'DATOS_INVALIDOS') {
    const todos = erroresDelServidor(error.problema?.campos)
    const propios = Object.fromEntries(Object.entries(todos).filter(([campo]) => campos.includes(campo)))
    return Object.keys(propios).length > 0 && Object.keys(propios).length === Object.keys(todos).length
      ? { campos: propios, general: null }
      : { campos: propios, general: 'perfil.errores.DATOS_INVALIDOS' }
  }
  return { campos: {}, general: 'perfil.errores.GENERICO' }
}

/**
 * WF-16 «Mi perfil» (RF53), solo para el Turista (RutaProtegida, DEC-92). Dos secciones en pestañas, «Datos
 * personales» e «Intereses turísticos», con la sección en la URL, más el acceso a «Mis favoritos» (WF-17).
 *
 * El PUT reemplaza el perfil completo (DEC-84): cada sección envía su borrador y los valores ya guardados de la otra,
 * así un guardado nunca confirma un cambio a medias de la otra sección; ese borrador se conserva (DEC-97). El correo se
 * muestra en solo lectura y nunca se envía. Las reglas son las del registro (RF01, DEC-85, DEC-98). Al guardar, el
 * nombre del encabezado se actualiza con /me. Los borradores sobreviven al cambio de pestaña y de idioma (RNF08).
 */
export function PerfilPage() {
  const { t } = useTranslation()
  const id = useId()
  const { refrescarUsuario } = useSesion()
  const paises = usePaises()
  const [parametros, setParametros] = useSearchParams()
  const seccion: Seccion = parametros.get('seccion') === 'intereses' ? 'intereses' : 'datos'

  const [carga, setCarga] = useState<Carga>({ estado: 'cargando' })
  const [intento, setIntento] = useState(0)
  const [datos, setDatos] = useState<DatosPersonales | null>(null)
  const [categorias, setCategorias] = useState<string[] | null>(null)
  const [errores, setErrores] = useState<ErroresCampos>({})
  const [estado, setEstado] = useState<Estado | null>(null)
  const [enviando, setEnviando] = useState(false)
  const pestanas = useRef(new Map<Seccion, HTMLButtonElement>())

  useEffect(() => {
    let vigente = true
    setCarga({ estado: 'cargando' })
    consultarPerfil()
      .then((perfil) => {
        if (!vigente) return
        setCarga({ estado: 'ok', guardado: perfil })
        setDatos(datosDe(perfil))
        setCategorias(perfil.categoriasInteres)
      })
      .catch(() => {
        // Un 401 ya cerró la sesión y RutaProtegida lleva al login; otro error se muestra con «Reintentar».
        if (vigente) setCarga({ estado: 'error' })
      })
    return () => {
      vigente = false
    }
  }, [intento])

  const cambiarSeccion = useCallback(
    (nueva: Seccion) => {
      if (nueva === seccion) return
      setErrores({})
      setEstado(null)
      setParametros(nueva === 'datos' ? new URLSearchParams() : new URLSearchParams({ seccion: nueva }))
    },
    [seccion, setParametros],
  )

  const alPulsarEnPestanas = (evento: KeyboardEvent<HTMLDivElement>) => {
    const actual = SECCIONES.indexOf(seccion)
    const destino: Record<string, number> = {
      ArrowRight: (actual + 1) % SECCIONES.length,
      ArrowLeft: (actual - 1 + SECCIONES.length) % SECCIONES.length,
      Home: 0,
      End: SECCIONES.length - 1,
    }
    if (!(evento.key in destino)) return
    evento.preventDefault()
    const nueva = SECCIONES[destino[evento.key]]
    cambiarSeccion(nueva)
    pestanas.current.get(nueva)?.focus()
  }

  if (carga.estado === 'cargando' || (carga.estado === 'ok' && (!datos || !categorias))) {
    return (
      <div className="perfil">
        <h1>{t('perfil.titulo')}</h1>
        <p className="estado-carga" role="status" aria-busy="true">
          {t('perfil.cargando')}
        </p>
      </div>
    )
  }
  if (carga.estado === 'error' || !datos || !categorias) {
    return (
      <div className="perfil">
        <h1>{t('perfil.titulo')}</h1>
        <div className="estado-catalogo estado-catalogo--error" role="alert">
          <p className="estado-catalogo__mensaje">{t('perfil.errores.carga')}</p>
          <button type="button" className="boton-secundario" onClick={() => setIntento((n) => n + 1)}>
            {t('catalogo.busqueda.reintentar')}
          </button>
        </div>
      </div>
    )
  }

  const { guardado } = carga
  const camposSeccion: readonly string[] = seccion === 'datos' ? CAMPOS_DATOS : ['categoriasInteres']

  const guardar = async (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault()
    if (enviando) return
    setEstado(null)
    // El borrador de esta sección más lo guardado de la otra (DEC-97).
    const perfil: DatosPerfil =
      seccion === 'datos'
        ? { ...datos, categoriasInteres: guardado.categoriasInteres }
        : { ...datosDe(guardado), categoriasInteres: categorias }
    const encontrados = Object.fromEntries(
      Object.entries(validarPerfil(perfil, paises.codigos)).filter(([campo]) => camposSeccion.includes(campo)),
    ) as ErroresCampos
    setErrores(encontrados)
    if (Object.keys(encontrados).length > 0) {
      enfocarPrimerError(camposSeccion, encontrados, ID_FOCO)
      return
    }
    setEnviando(true)
    try {
      const resultado = await actualizarPerfil(perfil)
      setCarga({ estado: 'ok', guardado: resultado })
      if (seccion === 'datos') setDatos(datosDe(resultado))
      else setCategorias(resultado.categoriasInteres)
      setEstado({ clave: seccion === 'datos' ? 'perfil.anuncios.datos' : 'perfil.anuncios.intereses', error: false })
      // El nombre del encabezado sale de /me; si /me falla (red o 5xx), queda el actual y el perfil ya está guardado.
      void refrescarUsuario()
    } catch (error) {
      const rechazo = aRechazoPerfil(error, camposSeccion)
      setErrores(rechazo.campos)
      if (rechazo.general) setEstado({ clave: rechazo.general, error: true })
      enfocarPrimerError(camposSeccion, rechazo.campos, ID_FOCO)
    } finally {
      setEnviando(false)
    }
  }

  const descartar = () => {
    setErrores({})
    setEstado(null)
    if (seccion === 'datos') setDatos(datosDe(guardado))
    else setCategorias(guardado.categoriasInteres)
  }

  const cambiarDato = (campo: keyof DatosPersonales, valor: string) =>
    setDatos((actuales) => (actuales ? { ...actuales, [campo]: valor } : actuales))
  const error = (campo: string) => textoErrorCampo(t, campo, errores[campo])

  return (
    <div className="perfil">
      <h1>{t('perfil.titulo')}</h1>

      <div className="perfil__navegacion">
        <div className="pestanas-perfil" role="tablist" aria-label={t('perfil.secciones')} onKeyDown={alPulsarEnPestanas}>
          {SECCIONES.map((opcion) => {
            const activa = opcion === seccion
            return (
              <button
                key={opcion}
                ref={(elemento) => {
                  if (elemento) pestanas.current.set(opcion, elemento)
                  else pestanas.current.delete(opcion)
                }}
                id={`${id}-pestana-${opcion}`}
                type="button"
                role="tab"
                aria-selected={activa}
                aria-controls={`${id}-panel`}
                tabIndex={activa ? 0 : -1}
                onClick={() => cambiarSeccion(opcion)}
              >
                {t(`perfil.pestanas.${opcion}`)}
              </button>
            )
          })}
        </div>
        <Link className="boton-secundario perfil__favoritos" to="/perfil/favoritos">
          {t('perfil.misFavoritos')}
        </Link>
      </div>

      <div id={`${id}-panel`} role="tabpanel" aria-labelledby={`${id}-pestana-${seccion}`} className="tarjeta perfil__panel">
        <form className="tarjeta__cuerpo" noValidate onSubmit={(evento) => void guardar(evento)}>
          {seccion === 'datos' ? (
            <>
              <Campo id={ID('correo')} etiqueta={t('perfil.correo')} type="email" value={guardado.correo} readOnly
                ayuda={t('perfil.correoAyuda')} />
              <div className="perfil__columnas">
                <Campo id={ID('primerNombre')} etiqueta={t('registro.primerNombre')} autoComplete="given-name"
                  value={datos.primerNombre} onChange={(e) => cambiarDato('primerNombre', e.target.value)}
                  error={error('primerNombre')} />
                <Campo id={ID('segundoNombre')} etiqueta={t('registro.segundoNombre')} opcional
                  autoComplete="additional-name" value={datos.segundoNombre}
                  onChange={(e) => cambiarDato('segundoNombre', e.target.value)} error={error('segundoNombre')} />
                <Campo id={ID('primerApellido')} etiqueta={t('registro.primerApellido')} autoComplete="family-name"
                  value={datos.primerApellido} onChange={(e) => cambiarDato('primerApellido', e.target.value)}
                  error={error('primerApellido')} />
                <Campo id={ID('segundoApellido')} etiqueta={t('registro.segundoApellido')} opcional
                  autoComplete="family-name" value={datos.segundoApellido}
                  onChange={(e) => cambiarDato('segundoApellido', e.target.value)} error={error('segundoApellido')} />
              </div>
              <SelectorPais id={ID('paisOrigen')} etiqueta={t('registro.paisOrigen')} paises={paises}
                valor={datos.paisOrigen} onCambiar={(codigo) => cambiarDato('paisOrigen', codigo)}
                error={error('paisOrigen')} />
            </>
          ) : (
            <CasillasCategorias prefijo={PREFIJO} leyenda={t('registro.categoriasInteres')}
              ayuda={t('perfil.ayudaCategorias')} seleccionadas={categorias} onCambiar={setCategorias}
              error={error('categoriasInteres')} />
          )}

          <div className="perfil__acciones">
            <button type="submit" className="boton-principal" disabled={enviando} aria-busy={enviando}>
              {enviando
                ? t('perfil.guardando')
                : t(seccion === 'datos' ? 'perfil.guardarDatos' : 'perfil.guardarIntereses')}
            </button>
            <button type="button" className="boton-secundario" onClick={descartar} disabled={enviando}>
              {t('perfil.descartar')}
            </button>
          </div>
          <p className={`perfil__estado${estado?.error ? ' perfil__estado--error' : ''}`} role="status">
            {estado && t(estado.clave)}
          </p>
        </form>
      </div>
    </div>
  )
}
