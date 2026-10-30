import { useEffect, useId, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router-dom'
import { Paginacion } from '../../shared/components/Paginacion'
import { SinPermiso } from '../../shared/components/SinPermiso'
import { formatearDia } from '../../shared/formato'
import { etiquetaCategoria } from '../catalogo/componentes/etiquetas'
import { InsigniaEstadoServicio } from './componentes/InsigniaEstadoServicio'
import { escribirCriteriosMisServicios, leerCriteriosMisServicios, type CriteriosMisServicios } from './criteriosMisServicios'
import { ESTADOS_PUBLICACION } from './serviciosPrestadorApi'
import { useMisServicios } from './useMisServicios'
import './prestador.css'

/**
 * WF-18 «Mis servicios» (flujo «Consultar mis servicios» de «Registrar servicio comercial», DEC-129), solo para el
 * Prestador (DEC-161). Filtro de estado y página en la URL (DEC-169), del más reciente al más antiguo, páginas de 10.
 * El estado se lee en texto («Pendiente de aprobación», N21); un Rechazado muestra su motivo y un Publicado, el
 * enlace a su ficha pública (WF-06).
 */
export function MisServiciosPage() {
  const { t, i18n } = useTranslation()
  const id = useId()
  const [parametros, setParametros] = useSearchParams()
  const criterios = useMemo(() => leerCriteriosMisServicios(parametros), [parametros])
  const lista = useMisServicios(criterios)

  // Un filtro o una página que no son válidos se normalizan en la URL.
  useEffect(() => {
    const normalizados = escribirCriteriosMisServicios(criterios)
    if (normalizados.toString() !== parametros.toString()) setParametros(normalizados, { replace: true })
  }, [criterios, parametros, setParametros])

  const datos = lista.estado === 'ok' ? lista.datos : null
  // Página fuera de rango (los servicios cambiaron): se va a la última, sin dejar la anterior en el historial.
  useEffect(() => {
    if (datos && datos.totalPaginas > 0 && criterios.pagina > datos.totalPaginas) {
      setParametros(escribirCriteriosMisServicios({ ...criterios, pagina: datos.totalPaginas }), { replace: true })
    }
  }, [datos, criterios, setParametros])

  const navegar = (cambios: Partial<CriteriosMisServicios>) =>
    setParametros(escribirCriteriosMisServicios({ ...criterios, ...cambios }))

  if (lista.estado === 'error' && lista.error === 'sinPermiso') return <SinPermiso />

  const vacia = datos !== null && datos.elementos.length === 0

  return (
    <div className="prestador">
      <div className="prestador__cabecera">
        <h1>{t('prestador.misServicios.titulo')}</h1>
        <Link className="boton-principal" to="/prestador/servicios/nuevo">
          {t('prestador.misServicios.registrar')}
        </Link>
      </div>

      <div className="campo prestador__filtro">
        <label htmlFor={`${id}-estado`}>{t('prestador.misServicios.filtroEstado')}</label>
        <div className="campo__control">
          <select
            id={`${id}-estado`}
            value={criterios.estado}
            onChange={(evento) => navegar({ estado: evento.target.value, pagina: 1 })}
          >
            <option value="">{t('prestador.misServicios.todos')}</option>
            {ESTADOS_PUBLICACION.map((estado) => (
              <option key={estado} value={estado}>
                {t(`prestador.estados.${estado}`)}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Región de estado de WF-18: total y lista vacía. Siempre presente. */}
      <div role="status" aria-live="polite" className="prestador__region">
        {datos && !vacia && <p>{t('prestador.misServicios.contador', { count: datos.totalElementos })}</p>}
        {vacia && datos && datos.totalElementos === 0 && (
          <p>{t(criterios.estado ? 'prestador.misServicios.vacioFiltro' : 'prestador.misServicios.vacio')}</p>
        )}
      </div>

      {lista.estado === 'cargando' && (
        <p className="estado-carga" role="status" aria-busy="true">
          {t('prestador.misServicios.cargando')}
        </p>
      )}
      {lista.estado === 'error' && (
        <div className="prestador__error-general" role="alert">
          <p>{t(`prestador.misServicios.errores.${lista.error}`)}</p>
          <button type="button" className="boton-secundario" onClick={lista.recargar}>
            {t('catalogo.busqueda.reintentar')}
          </button>
        </div>
      )}
      {vacia && datos?.totalElementos === 0 && !criterios.estado && (
        <p>
          <Link to="/prestador/servicios/nuevo">{t('prestador.misServicios.registrar')}</Link>
        </p>
      )}

      {datos && !vacia && (
        <div className="prestador__tabla-contenedor">
          {/* Roles explícitos: por debajo de 40 rem cada fila es una tarjeta (display: block) y conserva su semántica. */}
          <table className="prestador__tabla" role="table">
            <caption className="solo-lector">{t('prestador.misServicios.tabla')}</caption>
            <thead>
              <tr role="row">
                <th scope="col" role="columnheader">{t('prestador.misServicios.columnas.nombre')}</th>
                <th scope="col" role="columnheader">{t('prestador.misServicios.columnas.categoria')}</th>
                <th scope="col" role="columnheader">{t('prestador.misServicios.columnas.estado')}</th>
                <th scope="col" role="columnheader">{t('prestador.misServicios.columnas.registrado')}</th>
                <th scope="col" role="columnheader">{t('prestador.misServicios.columnas.accion')}</th>
              </tr>
            </thead>
            <tbody>
              {datos.elementos.map((servicio) => (
                <tr key={servicio.id} role="row">
                  <th scope="row" role="rowheader">{servicio.nombreComercial}</th>
                  <td role="cell" data-etiqueta={t('prestador.misServicios.columnas.categoria')}>
                    {etiquetaCategoria(t, servicio.categoria)}
                  </td>
                  <td role="cell" data-etiqueta={t('prestador.misServicios.columnas.estado')}>
                    <InsigniaEstadoServicio estado={servicio.estadoPublicacion} />
                    {servicio.estadoPublicacion === 'Rechazado' && servicio.motivoRechazo && (
                      <p className="prestador__motivo">
                        {t('prestador.misServicios.motivo', { motivo: servicio.motivoRechazo })}
                      </p>
                    )}
                  </td>
                  <td role="cell" data-etiqueta={t('prestador.misServicios.columnas.registrado')}>
                    {formatearDia(servicio.fechaRegistro, i18n.language)}
                  </td>
                  <td role="cell" data-etiqueta={t('prestador.misServicios.columnas.accion')}>
                    <div className="prestador__acciones-fila">
                      <Link
                        to={`/prestador/servicios/${encodeURIComponent(servicio.id)}`}
                        aria-label={t('prestador.misServicios.verDetalleDe', { nombre: servicio.nombreComercial })}
                      >
                        {t('prestador.misServicios.verDetalle')}
                      </Link>
                      {servicio.estadoPublicacion === 'Publicado' && (
                        <Link
                          to={`/servicios/${encodeURIComponent(servicio.id)}`}
                          aria-label={t('prestador.misServicios.verFichaDe', { nombre: servicio.nombreComercial })}
                        >
                          {t('prestador.misServicios.verFicha')}
                        </Link>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {datos && datos.totalPaginas > 1 && (
        <Paginacion actual={criterios.pagina} total={datos.totalPaginas} onCambiar={(pagina) => navegar({ pagina })} />
      )}
    </div>
  )
}
