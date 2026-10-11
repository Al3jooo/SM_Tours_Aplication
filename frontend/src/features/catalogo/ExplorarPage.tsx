import { useEffect, useId, useMemo, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router-dom'
import { IconoUbicacion } from '../../shared/components/Iconos'
import { EscenaSantaMarta } from '../../shared/components/Ilustraciones'
import { BotonProximamente } from '../../shared/components/Proximamente'
import type { CriteriosBusqueda, Orden, Ubicacion } from './catalogoApi'
import { EstadoCatalogo } from './componentes/EstadoCatalogo'
import { FiltrosOferta, type CambiosFiltros } from './componentes/FiltrosOferta'
import { Paginacion } from './componentes/Paginacion'
import { TarjetaOferta } from './componentes/TarjetaOferta'
import { escribirCriterios, leerCriterios } from './criteriosUrl'
import { useBusquedaOferta } from './useBusquedaOferta'
import './catalogo.css'

/** Órdenes que la API soporta; «Precio» y «Nombre» son Mejora de diseño (WF-05 no los muestra). */
const ORDENES_DISPONIBLES: Orden[] = ['PrecioAscendente', 'PrecioDescendente', 'Nombre']

/**
 * WF-05: exploración (pública). Los filtros, el orden y la página viven en la URL; la lista de resultados es el
 * contenido principal y el mapa es complementario. Parte 1: sin ubicación ni mapa (llegan en la parte 2).
 */
export function ExplorarPage() {
  const { t } = useTranslation()
  const id = useId()
  const [parametros, setParametros] = useSearchParams()
  const criterios = useMemo(() => leerCriterios(parametros), [parametros])
  const ubicacion: Ubicacion | undefined = undefined
  const busqueda = useBusquedaOferta(useMemo(() => ({ ...criterios, ubicacion }), [criterios, ubicacion]))

  const tituloResultados = useRef<HTMLHeadingElement>(null)
  const enfocarAlCargar = useRef(false)
  useEffect(() => {
    if (busqueda.estado !== 'cargando' && enfocarAlCargar.current) {
      enfocarAlCargar.current = false
      tituloResultados.current?.focus()
    }
  }, [busqueda.estado])

  const navegar = (nuevos: CriteriosBusqueda, enfocarResultados = false) => {
    enfocarAlCargar.current = enfocarResultados
    setParametros(escribirCriterios(nuevos))
  }
  const quitarFiltros = () => setParametros(new URLSearchParams())

  // Sin ubicación, un orden por distancia (p. ej. de un enlace compartido) no aplica: rige el de la API, por nombre.
  const ordenEfectivo: Orden =
    criterios.orden && (criterios.orden !== 'Distancia' || ubicacion) ? criterios.orden : ubicacion ? 'Distancia' : 'Nombre'

  const pagina = busqueda.estado === 'ok' ? busqueda.pagina : null

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

      <div className="explorar__cuerpo">
        <section className="explorar__resultados" aria-labelledby={`${id}-resultados`}>
          <div className="resultados__encabezado">
            <div aria-live="polite" aria-atomic="true">
              <h2 id={`${id}-resultados`} ref={tituloResultados} tabIndex={-1}>
                {pagina ? t('explorar.contador', { count: pagina.totalElementos }) : t('explorar.resultados')}
              </h2>
            </div>

            <div className="orden" role="group" aria-label={t('explorar.ordenar')}>
              <button
                type="button"
                className="orden__opcion deshabilitado"
                aria-disabled="true"
                aria-describedby={ubicacion ? undefined : `${id}-orden-sin-ubicacion`}
                onClick={(evento) => evento.preventDefault()}
              >
                {t('explorar.ordenes.Distancia')}
              </button>
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
                  {pagina.elementos.map((resultado) => (
                    <li key={`${resultado.tipo}-${resultado.id}`}>
                      <TarjetaOferta resultado={resultado} variante="lista" />
                    </li>
                  ))}
                </ol>
                <Paginacion
                  actual={criterios.pagina}
                  total={pagina.totalPaginas}
                  onCambiar={(numero) => navegar({ ...criterios, pagina: numero }, true)}
                />
              </>
            ))}
        </section>

        {/* Columna del mapa (complementaria). Parte 1: panel decorativo; el mapa Leaflet llega en la parte 2. */}
        <aside className="explorar__mapa" aria-labelledby={`${id}-mapa`}>
          <h2 id={`${id}-mapa`} className="explorar__mapa-titulo">
            {t('explorar.mapa')}
          </h2>
          <div className="explorar__mapa-lienzo" aria-hidden="true">
            <EscenaSantaMarta />
          </div>
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
          <BotonProximamente className="boton-secundario">
            <IconoUbicacion />
            <span>{t('explorar.centrar')}</span>
          </BotonProximamente>
        </aside>
      </div>

      <p className="explorar__asistente">
        {t('explorar.preguntaAsistente')}{' '}
        <BotonProximamente className="boton-secundario">{t('explorar.preguntarAsistente')}</BotonProximamente>
      </p>
    </div>
  )
}
