import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, ERROR_RED, servicioNoDisponible } from '../../shared/api/httpClient'
import { erroresAnidados, idDeCampo, type ErroresServicio } from '../prestador/rutasCampos'
import { registrarAtractivo } from './atractivosGestorApi'
import { borradorVacio, construirDatos, rutasConControl, validarBorrador, type BorradorAtractivo } from './borradorAtractivo'
import { DatoNoDisponible, FormularioAtractivo } from './componentes/FormularioAtractivo'
import { SelectorImagenes, elegirImagenes, type ImagenElegida } from './componentes/SelectorImagenes'
import type { EstadoAnuncioGestor } from './criteriosAtractivosGestor'
import { MAXIMO_IMAGENES, motivoCantidad } from './imagenesAtractivo'
import '../prestador/prestador.css'
import './gestor.css'

const esRutaImagen = (ruta: string) => ruta === 'imagenes' || ruta.startsWith('imagenes[')

/**
 * WF-09 «Registrar atractivo turístico» (CU «Registrar atractivo», con «Cargar imágenes»; RF06, RF07, RF24, RF27,
 * RF33, RF58), solo para el Gestor (DEC-220, DEC-233). Validación en el cliente de los datos y de cada imagen (firma,
 * 5 MB, de 1 a 5; DEC-226); el envío es un multipart (DEC-216, DEC-230). El 201 vuelve al panel con «Atractivo
 * registrado»: queda publicado de inmediato (DEC-87). Un 400 marca cada control por su ruta (también `imagenes[i]`),
 * un 413 CARGA_DEMASIADO_GRANDE es un mensaje general, y ninguno borra lo escrito ni las imágenes elegidas. La casilla de
 * veracidad va deshabilitada (DEC-219, DEC-228).
 */
export function RegistrarAtractivoPage() {
  const { t } = useTranslation()
  const id = useId()
  const navegar = useNavigate()
  const [borrador, setBorrador] = useState<BorradorAtractivo>(borradorVacio)
  const [imagenes, setImagenes] = useState<ImagenElegida[]>([])
  const [excedidas, setExcedidas] = useState(0)
  const [errores, setErrores] = useState<ErroresServicio>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const mensajeGeneral = useRef<HTMLParagraphElement>(null)
  const vistas = useRef<string[]>([])
  vistas.current = imagenes.map((imagen) => imagen.vistaPrevia)

  // Las vistas previas se liberan al salir de la página.
  useEffect(() => () => vistas.current.forEach((url) => URL.revokeObjectURL(url)), [])

  /** Los errores de las imágenes salen de sus motivos: se recalculan con cada cambio de la lista. */
  const conErroresDeImagenes = (actuales: ErroresServicio, lista: ImagenElegida[]): ErroresServicio => {
    const resto = Object.fromEntries(Object.entries(actuales).filter(([ruta]) => !esRutaImagen(ruta)))
    lista.forEach((imagen, indice) => {
      if (imagen.motivo) resto[`imagenes[${indice}]`] = imagen.motivo
    })
    return resto
  }
  const reemplazarImagenes = (lista: ImagenElegida[]) => {
    setImagenes(lista)
    setErrores((actuales) => conErroresDeImagenes(actuales, lista))
  }

  const cambiar = (cambios: Partial<BorradorAtractivo>, rutas: string[]) => {
    setBorrador((actual) => ({ ...actual, ...cambios }))
    setErrores((actuales) => Object.fromEntries(Object.entries(actuales).filter(([ruta]) => !rutas.includes(ruta))))
  }

  const agregar = async (archivos: File[]) => {
    const libres = Math.max(0, MAXIMO_IMAGENES - imagenes.length)
    setExcedidas(Math.max(0, archivos.length - libres))
    const nuevas = await elegirImagenes(archivos.slice(0, libres))
    setImagenes((actuales) => {
      const lista = [...actuales, ...nuevas]
      setErrores((previos) => conErroresDeImagenes(previos, lista))
      return lista
    })
  }

  const quitar = (indice: number) => {
    const quitada = imagenes[indice]
    if (quitada) URL.revokeObjectURL(quitada.vistaPrevia)
    setExcedidas(0)
    reemplazarImagenes(imagenes.filter((_, actual) => actual !== indice))
  }

  const enfocarPrimero = (encontrados: ErroresServicio) => {
    const primera = rutasConControl(true, imagenes.length).find((ruta) => encontrados[ruta])
    if (primera) setTimeout(() => document.getElementById(idDeCampo(primera))?.focus(), 0)
  }

  const enviar = async (evento: FormEvent) => {
    evento.preventDefault()
    if (enviando) return
    setGeneral(null)
    const locales = conErroresDeImagenes(validarBorrador(borrador, true), imagenes)
    const cantidad = motivoCantidad(imagenes.length)
    if (cantidad) locales.imagenes = cantidad
    setErrores(locales)
    if (Object.keys(locales).length > 0) {
      enfocarPrimero(locales)
      return
    }
    setEnviando(true)
    try {
      await registrarAtractivo(
        construirDatos(borrador, true),
        imagenes.map((imagen) => imagen.archivo),
      )
      const estado: EstadoAnuncioGestor = { anuncio: 'registrado' }
      navegar('/gestor/atractivos', { state: estado })
    } catch (fallo) {
      // Un 401 ya cerró la sesión: RutaProtegida lleva al login.
      if (fallo instanceof ApiError && fallo.status === 401) return
      const conControl = rutasConControl(true, imagenes.length)
      let clave: string | null = 'GENERICO'
      let campos: ErroresServicio = {}
      if (fallo instanceof ApiError) {
        if (fallo.codigo === 'DATOS_INVALIDOS') {
          const todos = erroresAnidados(fallo.problema?.campos)
          campos = Object.fromEntries(Object.entries(todos).filter(([ruta]) => conControl.includes(ruta)))
          const sinControl = Object.keys(todos).length - Object.keys(campos).length
          clave = sinControl > 0 || Object.keys(todos).length === 0 ? 'DATOS_INVALIDOS' : null
        } else if (fallo.status === 413 || fallo.codigo === 'CARGA_DEMASIADO_GRANDE') clave = 'CARGA_DEMASIADO_GRANDE'
        else if (fallo.status === 403 && fallo.codigo === 'ACCESO_DENEGADO') clave = 'ACCESO_DENEGADO'
        else if (servicioNoDisponible(fallo)) clave = 'SERVICIO_NO_DISPONIBLE'
        else if (fallo.codigo === ERROR_RED) clave = 'ERROR_RED'
      }
      setErrores(campos)
      setGeneral(clave)
      if (Object.keys(campos).length > 0 && !clave) enfocarPrimero(campos)
      else setTimeout(() => mensajeGeneral.current?.focus(), 0)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="prestador gestor">
      <nav aria-label={t('gestor.migas')} className="prestador__migas">
        <ol>
          <li>
            <Link to="/gestor/atractivos">{t('gestor.panel.titulo')}</Link>
          </li>
          <li aria-current="page">{t('gestor.registro.titulo')}</li>
        </ol>
      </nav>
      <h1>{t('gestor.registro.titulo')}</h1>
      <p className="prestador__apoyo">{t('gestor.registro.apoyo')}</p>

      <form className="prestador__formulario" noValidate onSubmit={(evento) => void enviar(evento)} aria-labelledby={`${id}-titulo`}>
        <span id={`${id}-titulo`} hidden>
          {t('gestor.registro.titulo')}
        </span>
        <FormularioAtractivo
          borrador={borrador}
          errores={errores}
          conEstado
          onCambiar={cambiar}
          fotos={
            <SelectorImagenes
              imagenes={imagenes}
              errores={errores}
              excedidas={excedidas}
              onAgregar={(archivos) => void agregar(archivos)}
              onQuitar={quitar}
            />
          }
        />

        <DatoNoDisponible etiqueta={t('gestor.registro.veracidad')} />

        <p ref={mensajeGeneral} tabIndex={-1} className={general ? 'prestador__error-general' : undefined} role="alert">
          {general && t(`gestor.registro.errores.${general}`)}
        </p>

        <div className="prestador__acciones">
          <button type="submit" className="boton-principal" disabled={enviando} aria-busy={enviando || undefined}>
            {enviando ? t('gestor.registro.enviando') : t('gestor.registro.enviar')}
          </button>
          <Link className="boton-secundario" to="/gestor/atractivos">
            {t('gestor.registro.cancelar')}
          </Link>
        </div>
      </form>

      <aside className="gestor__consejos" aria-label={t('gestor.consejos.titulo')}>
        <h2>{t('gestor.consejos.titulo')}</h2>
        <ul>
          <li>
            <strong>{t('gestor.consejos.aforo')}</strong> {t('gestor.consejos.aforoTexto')}
          </li>
          <li>
            <strong>{t('gestor.consejos.normas')}</strong> {t('gestor.consejos.normasTexto')}
          </li>
          <li>
            <strong>{t('gestor.consejos.estado')}</strong> {t('gestor.consejos.estadoTexto')}
          </li>
        </ul>
      </aside>
    </div>
  )
}
