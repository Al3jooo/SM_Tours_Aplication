import { useEffect, useId, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError, ERROR_RED, servicioNoDisponible } from '../../shared/api/httpClient'
import { listarPreguntas, type PreguntaFrecuente } from './contenidoApi'
import { filtrarPreguntas, indexarPreguntas } from './filtroPreguntas'
import './contenido.css'

type ErrorCarga = 'noDisponible' | 'red' | 'otro'

type Estado = { estado: 'cargando' } | { estado: 'ok'; preguntas: PreguntaFrecuente[] } | { estado: 'error'; error: ErrorCarga }

function clasificar(error: unknown): ErrorCarga {
  if (error instanceof ApiError && servicioNoDisponible(error)) return 'noDisponible'
  if (error instanceof ApiError && error.codigo === ERROR_RED) return 'red'
  return 'otro'
}

/**
 * WF-13, sección pública de preguntas frecuentes (CU «Consultar preguntas frecuentes», RF26, y su extensión «Buscar por
 * palabra clave», RF47). Pide la lista completa sin token (DEC-47) y la muestra en el orden del servidor (DEC-63). El
 * buscador filtra en el cliente con cada pulsación, sin retardo (DEC-62, DEC-71); la región `status` anuncia cuántas
 * quedan. Acordeón WAI-ARIA: se pueden abrir varias, y lo abierto se recuerda por id aunque cambie el filtro.
 */
export function PreguntasFrecuentesPage() {
  const { t } = useTranslation()
  const id = useId()
  const buscador = useRef<HTMLInputElement>(null)
  const [estado, setEstado] = useState<Estado>({ estado: 'cargando' })
  const [intento, setIntento] = useState(0)
  const [termino, setTermino] = useState('')
  const [abiertas, setAbiertas] = useState<Set<string>>(new Set())

  useEffect(() => {
    let vigente = true
    setEstado({ estado: 'cargando' })
    listarPreguntas()
      .then((preguntas) => {
        if (vigente) setEstado({ estado: 'ok', preguntas })
      })
      .catch((error: unknown) => {
        if (vigente) setEstado({ estado: 'error', error: clasificar(error) })
      })
    return () => {
      vigente = false
    }
  }, [intento])

  const preguntas = estado.estado === 'ok' ? estado.preguntas : null
  // Título y cuerpo se normalizan una vez por lista; cada pulsación solo normaliza el término (DEC-71).
  const indice = useMemo(() => indexarPreguntas(preguntas ?? []), [preguntas])
  const visibles = useMemo(() => filtrarPreguntas(indice, termino), [indice, termino])
  const hayTermino = termino.trim() !== ''
  const sinCoincidencias = preguntas !== null && preguntas.length > 0 && visibles.length === 0

  const alternar = (idPregunta: string) =>
    setAbiertas((actuales) => {
      const nuevas = new Set(actuales)
      if (!nuevas.delete(idPregunta)) nuevas.add(idPregunta)
      return nuevas
    })

  const borrarBusqueda = () => {
    setTermino('')
    buscador.current?.focus()
  }

  return (
    <div className="preguntas-frecuentes">
      <h1>{t('faq.titulo')}</h1>
      <p className="preguntas-frecuentes__apoyo">{t('faq.apoyo')}</p>

      {estado.estado === 'cargando' && (
        <p className="estado-carga" role="status" aria-busy="true">
          {t('faq.cargando')}
        </p>
      )}

      {estado.estado === 'error' && (
        <div className="aviso-contenido aviso-contenido--error" role="alert">
          <p>{t(`faq.errores.${estado.error}`)}</p>
          <button type="button" className="boton-secundario" onClick={() => setIntento((n) => n + 1)}>
            {t('faq.reintentar')}
          </button>
        </div>
      )}

      {preguntas !== null && preguntas.length === 0 && <p className="aviso-contenido">{t('faq.vacia')}</p>}

      {preguntas !== null && preguntas.length > 0 && (
        <>
          <form
            className="preguntas-frecuentes__buscador"
            role="search"
            aria-label={t('faq.buscarEtiqueta')}
            onSubmit={(evento) => evento.preventDefault()}
          >
            <div className="campo">
              <label htmlFor={`${id}-buscar`}>{t('faq.buscar')}</label>
              <div className="campo__control">
                <input
                  ref={buscador}
                  id={`${id}-buscar`}
                  type="search"
                  value={termino}
                  placeholder={t('faq.buscarPlaceholder')}
                  aria-describedby={`${id}-ayuda`}
                  onChange={(evento) => setTermino(evento.target.value)}
                />
              </div>
              <p id={`${id}-ayuda`} className="preguntas-frecuentes__ayuda">
                {t('faq.ayudaBuscar')}
              </p>
            </div>
          </form>

          {/* Siempre presente: anuncia el número de resultados con cada cambio del término (RNF06, WCAG 4.1.3). */}
          <div className="preguntas-frecuentes__region" role="status" aria-live="polite">
            {sinCoincidencias ? (
              <p>{t('faq.sinCoincidencias', { termino: termino.trim() })}</p>
            ) : (
              hayTermino && <p>{t('faq.resultados', { n: visibles.length, total: preguntas.length })}</p>
            )}
          </div>
          {sinCoincidencias && (
            <button type="button" className="boton-secundario" onClick={borrarBusqueda}>
              {t('faq.borrarBusqueda')}
            </button>
          )}

          <ul className="acordeon-faq">
            {visibles.map((pregunta) => {
              const abierta = abiertas.has(pregunta.id)
              const panel = `${id}-panel-${pregunta.id}`
              return (
                <li key={pregunta.id} className="acordeon-faq__item">
                  <h2 className="acordeon-faq__titulo">
                    <button
                      type="button"
                      className="acordeon-faq__boton"
                      aria-expanded={abierta}
                      aria-controls={panel}
                      onClick={() => alternar(pregunta.id)}
                    >
                      <span>{pregunta.titulo}</span>
                      <span className="acordeon-faq__signo" aria-hidden="true">
                        {abierta ? '−' : '+'}
                      </span>
                    </button>
                  </h2>
                  <div id={panel} className="acordeon-faq__cuerpo" hidden={!abierta}>
                    <p>{pregunta.cuerpo}</p>
                  </div>
                </li>
              )
            })}
          </ul>
        </>
      )}
    </div>
  )
}
