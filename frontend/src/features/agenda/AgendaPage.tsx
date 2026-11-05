import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiError, ERROR_RED, servicioNoDisponible } from '../../shared/api/httpClient'
import { Paginacion } from '../../shared/components/Paginacion'
import { listarAgenda, type PaginaEventos } from './agendaApi'
import { formatearRango, partesDia } from './fechasEvento'
import './agenda.css'

type ErrorCarga = 'noDisponible' | 'red' | 'otro'

type Estado = { estado: 'cargando' } | { estado: 'ok'; datos: PaginaEventos } | { estado: 'error'; error: ErrorCarga }

function clasificar(error: unknown): ErrorCarga {
  if (error instanceof ApiError && servicioNoDisponible(error)) return 'noDisponible'
  if (error instanceof ApiError && error.codigo === ERROR_RED) return 'red'
  return 'otro'
}

/** `pagina` de la URL, desde 1; un valor que no es válido se lee como 1 (y no se escribe), así el 400 no ocurre. */
function leerPagina(parametros: URLSearchParams): number {
  const pagina = Number(parametros.get('pagina'))
  return Number.isInteger(pagina) && pagina >= 1 ? pagina : 1
}

function escribirPagina(pagina: number): URLSearchParams {
  return new URLSearchParams(pagina > 1 ? { pagina: String(pagina) } : {})
}

/**
 * WF-26 «Agenda de eventos» (CU «Consultar agenda de eventos», RF25), pública: se pide sin token (DEC-47, DEC-239) y
 * se llega desde «Agenda de eventos» del pie (DEC-246). Muestra la página **tal como la devuelve el servidor**, que ya
 * filtró los vigentes con «hoy» en Bogotá: el cliente no recalcula «hoy» ni marca «en curso» (DEC-247). Tarjetas con
 * el bloque del día de inicio, el rango, la ubicación y la descripción con sus saltos de línea. Vacía, página fuera de
 * rango y errores, como DEC-249.
 */
export function AgendaPage() {
  const { t, i18n } = useTranslation()
  const [parametros, setParametros] = useSearchParams()
  const pagina = leerPagina(parametros)
  const [estado, setEstado] = useState<Estado>({ estado: 'cargando' })
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vigente = true
    setEstado({ estado: 'cargando' })
    listarAgenda(pagina)
      .then((datos) => {
        if (vigente) setEstado({ estado: 'ok', datos })
      })
      .catch((error: unknown) => {
        if (vigente) setEstado({ estado: 'error', error: clasificar(error) })
      })
    return () => {
      vigente = false
    }
  }, [pagina, intento])

  // Una página que no es válida se normaliza en la URL; una fuera de rango, a la última (FA2).
  useEffect(() => {
    const normalizados = escribirPagina(pagina)
    if (normalizados.toString() !== parametros.toString()) setParametros(normalizados, { replace: true })
  }, [pagina, parametros, setParametros])

  const datos = estado.estado === 'ok' ? estado.datos : null
  useEffect(() => {
    if (datos && datos.totalPaginas > 0 && pagina > datos.totalPaginas) {
      setParametros(escribirPagina(datos.totalPaginas), { replace: true })
    }
  }, [datos, pagina, setParametros])

  const vacia = datos !== null && datos.totalElementos === 0

  return (
    <div className="agenda">
      <h1>{t('agenda.titulo')}</h1>
      <p className="agenda__apoyo">{t('agenda.apoyo')}</p>

      {/* Región de estado: total y agenda vacía. Siempre presente. */}
      <div className="agenda__region" role="status" aria-live="polite">
        {datos && !vacia && <p>{t('agenda.contador', { count: datos.totalElementos })}</p>}
      </div>

      {estado.estado === 'cargando' && (
        <p className="estado-carga" role="status" aria-busy="true">
          {t('agenda.cargando')}
        </p>
      )}

      {estado.estado === 'error' && (
        <div className="agenda__aviso agenda__aviso--error" role="alert">
          <p>{t(`agenda.errores.${estado.error}`)}</p>
          <button type="button" className="boton-secundario" onClick={() => setIntento((n) => n + 1)}>
            {t('catalogo.busqueda.reintentar')}
          </button>
        </div>
      )}

      {vacia && (
        <div className="agenda__aviso">
          <p>{t('agenda.vacia')}</p>
          <Link to="/explorar">{t('agenda.explorar')}</Link>
        </div>
      )}

      {datos && datos.elementos.length > 0 && (
        <ul className="agenda__lista" aria-label={t('agenda.lista')}>
          {datos.elementos.map((evento) => {
            const { dia, mes } = partesDia(evento.fechaInicio, i18n.language)
            return (
              <li key={evento.id}>
                <article className="agenda__evento" aria-labelledby={`evento-${evento.id}`}>
                  <p className="agenda__dia" aria-hidden="true">
                    <span className="agenda__dia-numero">{dia}</span>
                    <span className="agenda__dia-mes">{mes}</span>
                  </p>
                  <div className="agenda__cuerpo">
                    <h2 id={`evento-${evento.id}`}>{evento.nombre}</h2>
                    <dl className="agenda__datos">
                      <div>
                        <dt>{t('agenda.fechas')}</dt>
                        <dd>{formatearRango(evento.fechaInicio, evento.fechaFin, i18n.language)}</dd>
                      </div>
                      <div>
                        <dt>{t('agenda.ubicacion')}</dt>
                        <dd>{evento.ubicacion}</dd>
                      </div>
                    </dl>
                    <p className="agenda__descripcion">{evento.descripcion}</p>
                  </div>
                </article>
              </li>
            )
          })}
        </ul>
      )}

      {datos && datos.totalPaginas > 1 && (
        <Paginacion actual={pagina} total={datos.totalPaginas} onCambiar={(nueva) => setParametros(escribirPagina(nueva))} />
      )}
    </div>
  )
}
