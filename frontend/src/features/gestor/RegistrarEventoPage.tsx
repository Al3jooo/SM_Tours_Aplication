import { useId, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, ERROR_RED, servicioNoDisponible } from '../../shared/api/httpClient'
import { CampoServicio } from '../prestador/componentes/CampoServicio'
import { erroresAnidados, idDeCampo, type ErroresServicio } from '../prestador/rutasCampos'
import {
  MAXIMO_EVENTO,
  RUTAS_EVENTO,
  borradorEventoVacio,
  cuerpoEvento,
  textoErrorEvento,
  validarBorradorEvento,
  type BorradorEvento,
} from './borradorEvento'
import type { EstadoAnuncioGestor } from './criteriosAtractivosGestor'
import { registrarEvento } from './eventosGestorApi'
import '../prestador/prestador.css'
import './gestor.css'

/**
 * WF-25 «Registrar evento» (CU «Registrar evento en agenda», RF25), solo para el Gestor (DEC-220, DEC-245). Página
 * propia, como WF-09. El cliente valida los mismos límites que ms-catalog y que el fin no sea anterior al inicio
 * (DEC-248); «no anterior a hoy» lo decide el servidor (DEC-242). El 201 vuelve al panel con su anuncio: el evento
 * queda publicado de inmediato. Un 400 marca cada control; ningún error borra lo escrito (DEC-249).
 */
export function RegistrarEventoPage() {
  const { t } = useTranslation()
  const id = useId()
  const navegar = useNavigate()
  const [borrador, setBorrador] = useState<BorradorEvento>(borradorEventoVacio)
  const [errores, setErrores] = useState<ErroresServicio>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const mensajeGeneral = useRef<HTMLParagraphElement>(null)

  const cambiar = (ruta: keyof BorradorEvento, valor: string) => {
    setBorrador((actual) => ({ ...actual, [ruta]: valor }))
    // Cambiar una fecha puede resolver el error de orden de la otra.
    const resueltas: string[] = ruta === 'fechaInicio' || ruta === 'fechaFin' ? ['fechaInicio', 'fechaFin'] : [ruta]
    setErrores((actuales) => Object.fromEntries(Object.entries(actuales).filter(([r]) => !resueltas.includes(r))))
  }

  const enfocarPrimero = (encontrados: ErroresServicio) => {
    const primera = RUTAS_EVENTO.find((ruta) => encontrados[ruta])
    if (primera) setTimeout(() => document.getElementById(idDeCampo(primera))?.focus(), 0)
  }

  const enviar = async (evento: FormEvent) => {
    evento.preventDefault()
    if (enviando) return
    setGeneral(null)
    const locales = validarBorradorEvento(borrador)
    setErrores(locales)
    if (Object.keys(locales).length > 0) {
      enfocarPrimero(locales)
      return
    }
    setEnviando(true)
    try {
      await registrarEvento(cuerpoEvento(borrador))
      const estado: EstadoAnuncioGestor = { anuncio: 'registrado' }
      navegar('/gestor/eventos', { state: estado })
    } catch (fallo) {
      // Un 401 ya cerró la sesión: RutaProtegida lleva al login.
      if (fallo instanceof ApiError && fallo.status === 401) return
      let clave: string | null = 'GENERICO'
      let campos: ErroresServicio = {}
      if (fallo instanceof ApiError) {
        if (fallo.codigo === 'DATOS_INVALIDOS') {
          const todos = erroresAnidados(fallo.problema?.campos)
          campos = Object.fromEntries(Object.entries(todos).filter(([ruta]) => (RUTAS_EVENTO as readonly string[]).includes(ruta)))
          const sinControl = Object.keys(todos).length - Object.keys(campos).length
          clave = sinControl > 0 || Object.keys(todos).length === 0 ? 'DATOS_INVALIDOS' : null
        } else if (fallo.status === 403 && fallo.codigo === 'ACCESO_DENEGADO') clave = 'ACCESO_DENEGADO'
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

  const error = (ruta: keyof BorradorEvento) => textoErrorEvento(t, ruta, errores[ruta], borrador)

  return (
    <div className="prestador gestor">
      <nav aria-label={t('gestor.migas')} className="prestador__migas">
        <ol>
          <li>
            <Link to="/gestor/eventos">{t('gestor.eventos.panel.titulo')}</Link>
          </li>
          <li aria-current="page">{t('gestor.eventos.registro.titulo')}</li>
        </ol>
      </nav>
      <h1>{t('gestor.eventos.registro.titulo')}</h1>
      <p className="prestador__apoyo">{t('gestor.eventos.registro.apoyo')}</p>

      <form className="prestador__formulario" noValidate onSubmit={(evento) => void enviar(evento)} aria-labelledby={`${id}-titulo`}>
        <span id={`${id}-titulo`} hidden>
          {t('gestor.eventos.registro.titulo')}
        </span>
        <section className="tarjeta prestador__seccion" aria-labelledby={`${id}-datos`}>
          <h2 id={`${id}-datos`}>{t('gestor.eventos.registro.datos')}</h2>
          <CampoServicio
            ruta="nombre"
            etiqueta={t('gestor.eventos.campos.nombre')}
            maximo={MAXIMO_EVENTO.nombre}
            valor={borrador.nombre}
            onCambiar={(valor) => cambiar('nombre', valor)}
            error={error('nombre')}
          />
          <div className="prestador__columnas">
            <CampoServicio
              ruta="fechaInicio"
              type="date"
              etiqueta={t('gestor.eventos.campos.fechaInicio')}
              valor={borrador.fechaInicio}
              onCambiar={(valor) => cambiar('fechaInicio', valor)}
              error={error('fechaInicio')}
            />
            <CampoServicio
              ruta="fechaFin"
              type="date"
              etiqueta={t('gestor.eventos.campos.fechaFin')}
              ayuda={t('gestor.eventos.registro.fechasAyuda')}
              valor={borrador.fechaFin}
              onCambiar={(valor) => cambiar('fechaFin', valor)}
              error={error('fechaFin')}
            />
          </div>
          <CampoServicio
            ruta="ubicacion"
            etiqueta={t('gestor.eventos.campos.ubicacion')}
            maximo={MAXIMO_EVENTO.ubicacion}
            valor={borrador.ubicacion}
            onCambiar={(valor) => cambiar('ubicacion', valor)}
            error={error('ubicacion')}
          />
          <CampoServicio
            ruta="descripcion"
            multilinea
            etiqueta={t('gestor.eventos.campos.descripcion')}
            maximo={MAXIMO_EVENTO.descripcion}
            valor={borrador.descripcion}
            onCambiar={(valor) => cambiar('descripcion', valor)}
            error={error('descripcion')}
          />
        </section>

        <p className="prestador__nota">{t('gestor.eventos.registro.publicacion')}</p>

        <p ref={mensajeGeneral} tabIndex={-1} className={general ? 'prestador__error-general' : undefined} role="alert">
          {general && t(`gestor.eventos.registro.errores.${general}`)}
        </p>

        <div className="prestador__acciones">
          <button type="submit" className="boton-principal" disabled={enviando} aria-busy={enviando || undefined}>
            {enviando ? t('gestor.eventos.registro.enviando') : t('gestor.eventos.registro.enviar')}
          </button>
          <Link className="boton-secundario" to="/gestor/eventos">
            {t('gestor.eventos.registro.cancelar')}
          </Link>
        </div>
      </form>
    </div>
  )
}
