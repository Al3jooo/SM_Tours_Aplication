import { useEffect, useId, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router-dom'
import { BotonProximamente } from '../../shared/components/Proximamente'
import type { CriteriosBusqueda, Orden, ResultadoOferta, Ubicacion } from './catalogoApi'
import { BotonUbicacion } from './componentes/BotonUbicacion'
import { EstadoCatalogo } from './componentes/EstadoCatalogo'
import { FiltrosOferta, type CambiosFiltros } from './componentes/FiltrosOferta'
import { MapaOferta, claveResultado } from './componentes/MapaOferta'
import { Paginacion } from './componentes/Paginacion'
import { TarjetaOferta } from './componentes/TarjetaOferta'
import { escribirCriterios, leerCriterios } from './criteriosUrl'
import { useBusquedaOferta } from './useBusquedaOferta'
import { useEsEscritorio } from './useEsEscritorio'
import { useUbicacion } from './useUbicacion'
import './catalogo.css'

/** Órdenes que la API soporta; «Precio» y «Nombre» son Mejora de diseño (WF-05 no los muestra). */
const ORDENES_DISPONIBLES: Orden[] = ['PrecioAscendente', 'PrecioDescendente', 'Nombre']

const SIN_RESULTADOS: ResultadoOferta[] = []

type Vista = 'lista' | 'mapa'

/**
 * WF-05: exploración (pública). Los filtros, el orden y la página viven en la URL; la ubicación, solo en memoria.
 * La lista de resultados es el contenido principal y el mapa es complementario: en escritorio se ven lado a lado; en
 * una columna, el selector «Lista / Mapa» muestra uno a la vez.
 */
export function ExplorarPage() {
  const { t } = useTranslation()
  const id = useId()
  const [parametros, setParametros] = useSearchParams()
  const criterios = useMemo(() => leerCriterios(parametros), [parametros])
  const geo = useUbicacion()
  const ubicacion: Ubicacion | undefined = geo.estado === 'disponible' ? geo.ubicacion : undefined
  const busqueda = useBusquedaOferta(useMemo(() => ({ ...criterios, ubicacion }), [criterios, ubicacion]))
  const esEscritorio = useEsEscritorio()
  const [vista, setVista] = useState<Vista>('lista')
  const [resaltada, setResaltada] = useState<string | null>(null)
  const tarjetas = useRef(new Map<string, HTMLElement>())

  const tituloResultados = useRef<HTMLHeadingElement>(null)
  const enfocarAlCargar = useRef(false)
  useEffect(() => {
    if (busqueda.estado !== 'cargando' && enfocarAlCargar.current) {
      enfocarAlCargar.current = false
      tituloResultados.current?.focus()
    }
  }, [busqueda.estado])

  // Con la ubicación nueva cambian el orden y las distancias: la página que se estaba viendo ya no corresponde.
  const ubicacionAtendida = useRef<Ubicacion | undefined>(undefined)
  useEffect(() => {
    if (!ubicacion || ubicacion === ubicacionAtendida.current) return
    ubicacionAtendida.current = ubicacion
    if (criterios.pagina > 1) setParametros(escribirCriterios({ ...criterios, pagina: 1 }), { replace: true })
  }, [ubicacion, criterios, setParametros])

  const navegar = (nuevos: CriteriosBusqueda, enfocarResultados = false) => {
    enfocarAlCargar.current = enfocarResultados
    setParametros(escribirCriterios(nuevos))
  }
  const quitarFiltros = () => setParametros(new URLSearchParams())

  // Sin orden en la URL rige el de la API: por distancia con ubicación y por nombre sin ella. Un orden por distancia
  // sin ubicación (p. ej. de un enlace compartido) no aplica.
  const ordenEfectivo: Orden =
    criterios.orden && (criterios.orden !== 'Distancia' || ubicacion) ? criterios.orden : ubicacion ? 'Distancia' : 'Nombre'

  const pagina = busqueda.estado === 'ok' ? busqueda.pagina : null
  const resultadosMapa = pagina ? pagina.elementos : busqueda.estado === 'error' ? SIN_RESULTADOS : null

  const seleccionarDesdeMapa = (clave: string) => {
    setResaltada(clave)
    const tarjeta = tarjetas.current.get(clave)
    tarjeta?.focus({ preventScroll: true })
    tarjeta?.scrollIntoView?.({ block: 'nearest' })
  }

  const botonUbicacion = <BotonUbicacion estado={geo.estado} onCentrar={geo.centrar} />

  return (
    <div className="explorar">
      <nav className="migas" aria-label={t('explorar.migas')}>
        <ol>
          <li>
            <Link to="/">Samaria360</Link>
          </li>
          <li>{t('explorar.migaExploracion')}</li>
          <li aria-current="page">{t('explorar.migaActual')}</li>
        </ol>
      </nav>
      <h1 className="explorar__titulo">{t('explorar.titulo')}</h1>
      <p className="explorar__apoyo">{t('explorar.apoyo')}</p>

      <FiltrosOferta
        key={parametros.toString()}
        criterios={criterios}
        hayUbicacion={ubicacion !== undefined}
        onAplicar={(cambios: CambiosFiltros) => navegar({ ...criterios, ...cambios, pagina: 1 })}
        onQuitarCategoria={() => navegar({ ...criterios, categoria: undefined, pagina: 1 })}
      />

      <div className="explorar__barra">
        {/* En una columna: selector de vista y botón de ubicación a mano también en la lista. */}
        <div className="vista-selector" role="group" aria-label={t('vista.selector')}>
          {(['lista', 'mapa'] as const).map((opcion) => (
            <button
              key={opcion}
              type="button"
              className="vista-selector__opcion"
              aria-pressed={vista === opcion}
              onClick={() => setVista(opcion)}
            >
              {t(`vista.${opcion}`)}
            </button>
          ))}
        </div>
        {!esEscritorio && botonUbicacion}
        <p role="status" className="aviso-ubicacion">
          {geo.estado === 'disponible' && (
            <>
              {t('ubicacion.usando')}
              {geo.lejos && <> {t('ubicacion.lejos')}</>}
            </>
          )}
          {geo.estado === 'error' && t(`ubicacion.${geo.motivo}`)}
        </p>
      </div>

      <div className="explorar__cuerpo" data-vista={vista}>
        <section className="explorar__resultados" aria-labelledby={`${id}-resultados`}>
          <div className="resultados__encabezado">
            <div aria-live="polite" aria-atomic="true">
              <h2 id={`${id}-resultados`} ref={tituloResultados} tabIndex={-1}>
                {pagina ? t('explorar.contador', { count: pagina.totalElementos }) : t('explorar.resultados')}
              </h2>
            </div>

            <div className="orden" role="group" aria-label={t('explorar.ordenar')}>
              {ubicacion ? (
                <button
                  type="button"
                  className="orden__opcion"
                  aria-pressed={ordenEfectivo === 'Distancia'}
                  onClick={() => navegar({ ...criterios, orden: 'Distancia', pagina: 1 })}
                >
                  {t('explorar.ordenes.Distancia')}
                </button>
              ) : (
                <button
                  type="button"
                  className="orden__opcion deshabilitado"
                  aria-disabled="true"
                  aria-describedby={`${id}-orden-sin-ubicacion`}
                  onClick={(evento) => evento.preventDefault()}
                >
                  {t('explorar.ordenes.Distancia')}
                </button>
              )}
              <BotonProximamente className="orden__opcion" etiquetaVisible={false}>
                {t('explorar.ordenes.Afluencia')}
              </BotonProximamente>
              <BotonProximamente className="orden__opcion" etiquetaVisible={false}>
                {t('explorar.ordenes.Calificacion')}
              </BotonProximamente>
              {ORDENES_DISPONIBLES.map((orden) => (
                <button
                  key={orden}
                  type="button"
                  className="orden__opcion"
                  aria-pressed={ordenEfectivo === orden}
                  onClick={() => navegar({ ...criterios, orden, pagina: 1 })}
                >
                  {t(`explorar.ordenes.${orden}`)}
                </button>
              ))}
            </div>
            {!ubicacion && (
              <p id={`${id}-orden-sin-ubicacion`} className="filtro__ayuda">
                {t('explorar.sinUbicacion')}
              </p>
            )}
          </div>

          {busqueda.estado === 'cargando' && <EstadoCatalogo tipo="cargando" />}
          {busqueda.estado === 'error' && (
            <EstadoCatalogo tipo={busqueda.error} onReintentar={busqueda.reintentar} onQuitarFiltros={quitarFiltros} />
          )}
          {pagina &&
            (pagina.elementos.length === 0 ? (
              pagina.totalElementos > 0 ? (
                <EstadoCatalogo tipo="fueraDeRango" onPrimeraPagina={() => navegar({ ...criterios, pagina: 1 }, true)} />
              ) : (
                <EstadoCatalogo tipo="vacio" onQuitarFiltros={quitarFiltros} />
              )
            ) : (
              <>
                <ol className="lista-resultados">
                  {pagina.elementos.map((resultado) => {
                    const clave = claveResultado(resultado)
                    return (
                      <li key={clave}>
                        <TarjetaOferta
                          ref={(elemento) => {
                            if (elemento) tarjetas.current.set(clave, elemento)
                            return () => {
                              tarjetas.current.delete(clave)
                            }
                          }}
                          resultado={resultado}
                          variante="lista"
                          resaltada={resaltada === clave}
                          onResaltar={(activa) =>
                            setResaltada((actual) => (activa ? clave : actual === clave ? null : actual))
                          }
                        />
                      </li>
                    )
                  })}
                </ol>
                <Paginacion
                  actual={criterios.pagina}
                  total={pagina.totalPaginas}
                  onCambiar={(numero) => navegar({ ...criterios, pagina: numero }, true)}
                />
              </>
            ))}
        </section>

        <aside className="explorar__mapa" aria-labelledby={`${id}-mapa`}>
          <h2 id={`${id}-mapa`} className="explorar__mapa-titulo">
            {t('explorar.mapa')}
          </h2>
          <MapaOferta
            resultados={resultadosMapa}
            ubicacion={ubicacion}
            ubicacionLejana={geo.estado === 'disponible' && geo.lejos}
            centrado={geo.centrado}
            visible={esEscritorio || vista === 'mapa'}
            resaltada={resaltada}
            onSeleccionar={seleccionarDesdeMapa}
          />
          <div className="leyenda-aforo">
            <p className="leyenda-aforo__titulo">
              {t('explorar.leyendaAforo')}: <span>{t('explorar.sinDatosAforo')}</span>
            </p>
            <ul aria-label={t('explorar.leyendaAforo')}>
              <li className="leyenda-aforo__item leyenda-aforo__item--bajo">{t('explorar.aforoBajo')}</li>
              <li className="leyenda-aforo__item leyenda-aforo__item--medio">{t('explorar.aforoMedio')}</li>
              <li className="leyenda-aforo__item leyenda-aforo__item--alto">{t('explorar.aforoAlto')}</li>
            </ul>
          </div>
          {esEscritorio && botonUbicacion}
        </aside>
      </div>

      <p className="explorar__asistente">
        {t('explorar.preguntaAsistente')}{' '}
        <BotonProximamente className="boton-secundario">{t('explorar.preguntarAsistente')}</BotonProximamente>
      </p>
    </div>
  )
}
