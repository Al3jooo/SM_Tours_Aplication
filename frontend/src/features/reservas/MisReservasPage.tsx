import { useEffect, useId, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { ApiError } from '../../shared/api/httpClient'
import { useSesion } from '../auth/useSesion'
import { Paginacion } from '../../shared/components/Paginacion'
import { DialogoCancelar } from './componentes/DialogoCancelar'
import { TarjetaReserva } from './componentes/TarjetaReserva'
import {
  escribirCriteriosReservas,
  hayFiltros,
  leerCriteriosReservas,
  validarCriteriosReservas,
  type CriteriosReservas,
} from './filtrosReservas'
import { cancelarReserva, ESTADOS_RESERVA, type EstadoReserva, type Reserva } from './reservasApi'
import { useMisReservas } from './useMisReservas'
import '../catalogo/catalogo.css'
import './reservas.css'

/**
 * WF-11 «Mis reservas» (pendiente de diseño: usa el lenguaje visual de WF-10). Lista del turista con filtros por fecha
 * de la actividad y estado (RF12), el estado de cada reserva (RF52), «Cancelar» con confirmación (RF18, RF44) y
 * «Compartir» en las Completadas (RF49), con «Enlace copiado» en la región de anuncios de la página.
 * La ruta es privada (RutaProtegida); con otro rol, un aviso.
 */
export function MisReservasPage() {
  const { t } = useTranslation()
  const { sesion } = useSesion()
  if (sesion.estado !== 'autenticado') return null
  if (sesion.usuario.rol !== 'Turista') {
    return (
      <div className="mis-reservas">
        <h1>{t('misReservas.titulo')}</h1>
        <p className="aviso-reserva" role="alert">
          {t('misReservas.soloTuristas')}
        </p>
      </div>
    )
  }
  return <ListaMisReservas />
}

function ListaMisReservas() {
  const { t } = useTranslation()
  const id = useId()
  const [parametros, setParametros] = useSearchParams()
  const criterios = useMemo(() => leerCriteriosReservas(parametros), [parametros])
  const fallos = validarCriteriosReservas(criterios)
  const consulta = useMisReservas(criterios, Object.keys(fallos).length === 0)
  const { recargar, reemplazar } = consulta

  const [anuncio, setAnuncio] = useState('')
  const [errorPagina, setErrorPagina] = useState<string | null>(null)
  const [aCancelar, setACancelar] = useState<Reserva | null>(null)
  const [cancelacion, setCancelacion] = useState<{ enviando: boolean; error: string | null }>({
    enviando: false,
    error: null,
  })
  const botones = useRef(new Map<string, HTMLButtonElement>())
  const titulos = useRef(new Map<string, HTMLHeadingElement>())
  const enfocarTitulo = useRef<string | null>(null)

  const filtrar = (cambios: Partial<CriteriosReservas>) => {
    setErrorPagina(null)
    setAnuncio('')
    setParametros(escribirCriteriosReservas({ ...criterios, ...cambios, pagina: 1 }))
  }

  const alternarEstado = (estado: EstadoReserva, marcado: boolean) =>
    filtrar({
      estados: marcado ? [...criterios.estados, estado] : criterios.estados.filter((actual) => actual !== estado),
    })

  // Después de cancelar, el botón ya no existe (la reserva quedó Cancelada): el foco va al título de su tarjeta.
  useEffect(() => {
    if (enfocarTitulo.current) {
      titulos.current.get(enfocarTitulo.current)?.focus()
      enfocarTitulo.current = null
    }
  })

  const confirmarCancelacion = async () => {
    if (!aCancelar) return
    setCancelacion({ enviando: true, error: null })
    try {
      const cancelada = await cancelarReserva(aCancelar.idReserva)
      reemplazar(cancelada)
      enfocarTitulo.current = cancelada.idReserva
      setACancelar(null)
      setCancelacion({ enviando: false, error: null })
      setAnuncio(t('misReservas.cancelada', { servicio: cancelada.nombreServicio }))
    } catch (error: unknown) {
      const codigo = error instanceof ApiError ? error.codigo : 'GENERICO'
      if (codigo === 'RESERVA_NO_CANCELABLE') {
        // RF52 la completó mientras tanto: se cierra el diálogo y se vuelve a consultar.
        setACancelar(null)
        setCancelacion({ enviando: false, error: null })
        setErrorPagina(t('misReservas.errores.RESERVA_NO_CANCELABLE'))
        recargar()
        return
      }
      setCancelacion({ enviando: false, error: codigo })
    }
  }

  const cerrarDialogo = () => {
    const reserva = aCancelar
    setACancelar(null)
    setCancelacion({ enviando: false, error: null })
    if (reserva) botones.current.get(reserva.idReserva)?.focus()
  }

  const errorHasta = fallos.hasta ?? (consulta.estado === 'error' ? consulta.campos.hasta : undefined)
  const errorDesde = consulta.estado === 'error' ? consulta.campos.desde : undefined
  const errorEstado = consulta.estado === 'error' ? consulta.campos.estado : undefined
  const ahora = Date.now()
  const pagina = consulta.estado === 'ok' ? consulta.pagina : null

  return (
    <div className="mis-reservas">
      <h1>{t('misReservas.titulo')}</h1>
      <p className="mis-reservas__apoyo">{t('misReservas.apoyo')}</p>

      <form className="mis-reservas__filtros" onSubmit={(evento) => evento.preventDefault()} noValidate>
        <fieldset className="reserva__grupo">
          <legend>{t('misReservas.filtros')}</legend>
          <div className="reserva__campos">
            <div className="campo">
              <label htmlFor={`${id}-desde`}>{t('misReservas.desde')}</label>
              <div className="campo__control">
                <input
                  id={`${id}-desde`}
                  type="date"
                  value={criterios.desde}
                  aria-invalid={errorDesde ? true : undefined}
                  aria-describedby={errorDesde ? `${id}-desde-error` : undefined}
                  onChange={(evento) => filtrar({ desde: evento.target.value })}
                />
              </div>
              {errorDesde && (
                <p id={`${id}-desde-error`} className="campo__error">
                  {t(`misReservas.validacion.${errorDesde}`, { defaultValue: t('misReservas.validacion.FORMATO') })}
                </p>
              )}
            </div>
            <div className="campo">
              <label htmlFor={`${id}-hasta`}>{t('misReservas.hasta')}</label>
              <div className="campo__control">
                <input
                  id={`${id}-hasta`}
                  type="date"
                  value={criterios.hasta}
                  min={criterios.desde || undefined}
                  aria-invalid={errorHasta ? true : undefined}
                  aria-describedby={errorHasta ? `${id}-hasta-error` : undefined}
                  onChange={(evento) => filtrar({ hasta: evento.target.value })}
                />
              </div>
              {errorHasta && (
                <p id={`${id}-hasta-error`} className="campo__error" role="alert">
                  {t(`misReservas.validacion.${errorHasta}`, { defaultValue: t('misReservas.validacion.FORMATO') })}
                </p>
              )}
            </div>
          </div>
          <fieldset className="mis-reservas__estados" aria-describedby={errorEstado ? `${id}-estado-error` : undefined}>
            <legend>{t('misReservas.estado')}</legend>
            <ul>
              {ESTADOS_RESERVA.map((estado) => (
                <li key={estado}>
                  <input
                    type="checkbox"
                    id={`${id}-${estado}`}
                    checked={criterios.estados.includes(estado)}
                    onChange={(evento) => alternarEstado(estado, evento.target.checked)}
                  />
                  <label htmlFor={`${id}-${estado}`}>{t(`misReservas.estados.${estado}`)}</label>
                </li>
              ))}
            </ul>
            {errorEstado && (
              <p id={`${id}-estado-error`} className="campo__error">
                {t(`misReservas.validacion.${errorEstado}`, { defaultValue: t('misReservas.validacion.FORMATO') })}
              </p>
            )}
          </fieldset>
          {hayFiltros(criterios) && (
            <button type="button" className="boton-secundario" onClick={() => setParametros(new URLSearchParams())}>
              {t('misReservas.quitarFiltros')}
            </button>
          )}
        </fieldset>
      </form>

      <section className="mis-reservas__resultados" aria-labelledby={`${id}-resultados`}>
        <div aria-live="polite" aria-atomic="true">
          <h2 id={`${id}-resultados`}>
            {pagina ? t('misReservas.contador', { count: pagina.totalElementos }) : t('misReservas.resultados')}
          </h2>
        </div>
        <p className="reserva__anuncio" role="status">
          {anuncio}
        </p>
        {errorPagina && (
          <p className="aviso-reserva" role="alert">
            {errorPagina}
          </p>
        )}

        {consulta.estado === 'cargando' && (
          <p className="estado-carga" role="status" aria-busy="true">
            {t('misReservas.cargando')}
          </p>
        )}
        {consulta.estado === 'error' && consulta.error !== 'datosInvalidos' && (
          <div className="estado-catalogo estado-catalogo--error" role="alert">
            <p className="estado-catalogo__mensaje">{t(`misReservas.errores.${consulta.error}`)}</p>
            <button type="button" className="boton-secundario" onClick={recargar}>
              {t('catalogo.busqueda.reintentar')}
            </button>
          </div>
        )}
        {pagina &&
          (pagina.elementos.length === 0 ? (
            <p className="estado-catalogo__mensaje">
              {hayFiltros(criterios) ? t('misReservas.sinResultados') : t('misReservas.sinReservas')}
            </p>
          ) : (
            <>
              <ol className="mis-reservas__lista">
                {pagina.elementos.map((reserva) => (
                  <li key={reserva.idReserva}>
                    <TarjetaReserva
                      reserva={reserva}
                      ahora={ahora}
                      onCancelar={(elegida) => {
                        setAnuncio('')
                        setCancelacion({ enviando: false, error: null })
                        setACancelar(elegida)
                      }}
                      onCompartido={(resultado) => setAnuncio(resultado === 'copiado' ? t('misReservas.compartir.copiado') : '')}
                      refTitulo={(elemento) => {
                        if (elemento) titulos.current.set(reserva.idReserva, elemento)
                        else titulos.current.delete(reserva.idReserva)
                      }}
                      refBoton={(elemento) => {
                        if (elemento) botones.current.set(reserva.idReserva, elemento)
                        else botones.current.delete(reserva.idReserva)
                      }}
                    />
                  </li>
                ))}
              </ol>
              <Paginacion
                actual={criterios.pagina}
                total={pagina.totalPaginas}
                onCambiar={(numero) => setParametros(escribirCriteriosReservas({ ...criterios, pagina: numero }))}
              />
            </>
          ))}
      </section>

      {aCancelar && (
        <DialogoCancelar
          reserva={aCancelar}
          enviando={cancelacion.enviando}
          error={cancelacion.error}
          onConfirmar={() => void confirmarCancelacion()}
          onCerrar={cerrarDialogo}
        />
      )}
    </div>
  )
}
