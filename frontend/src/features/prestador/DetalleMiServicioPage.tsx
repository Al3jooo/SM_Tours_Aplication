import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation, useParams } from 'react-router-dom'
import { BotonNoDisponible } from '../../shared/components/BotonNoDisponible'
import { SinPermiso } from '../../shared/components/SinPermiso'
import { formatearDia } from '../../shared/formato'
import { etiquetaCategoria } from '../catalogo/componentes/etiquetas'
import { centavos, formatearCentavos } from '../reservas/dinero'
import { InsigniaEstadoServicio } from './componentes/InsigniaEstadoServicio'
import type { ServicioGestion } from './serviciosPrestadorApi'
import { useMiServicio } from './useMisServicios'
import './prestador.css'

function Dato({ etiqueta, children }: { etiqueta: string; children: ReactNode }) {
  const { t } = useTranslation()
  const vacio = children === null || children === undefined || children === ''
  return (
    <div>
      <dt>{etiqueta}</dt>
      <dd>
        {vacio ? (
          <span className="deshabilitado">
            <span aria-hidden="true">—</span>
            <span className="solo-lector">{t('comun.sinDatos')}</span>
          </span>
        ) : (
          children
        )}
      </dd>
    </div>
  )
}

const comoTexto = (valor: unknown) => (valor === null || valor === undefined ? '' : String(valor))

/** Los datos tal como se enviaron (WF-18, detalle), con los que fijó el servidor (precio derivado y RNT). */
function DatosEnviados({ servicio }: { servicio: ServicioGestion }) {
  const { t, i18n } = useTranslation()
  const dinero = (valor: unknown) => {
    const valorCentavos = centavos(valor as number | string | null)
    return valorCentavos === null ? '' : formatearCentavos(valorCentavos, i18n.language)
  }
  const detalle = servicio.detalleCategoria ?? {}
  const unidades = Array.isArray(detalle.unidades) ? (detalle.unidades as Record<string, unknown>[]) : []
  const dias = servicio.oferta?.diasOperacion ?? []
  const salidas = servicio.oferta?.salidas ?? []

  return (
    <dl className="prestador__datos">
      <Dato etiqueta={t('prestador.campos.categoria')}>{etiquetaCategoria(t, servicio.categoria)}</Dato>
      <Dato etiqueta={t('prestador.campos.rnt')}>{servicio.rnt}</Dato>
      <Dato etiqueta={t('prestador.campos.descripcion')}>{servicio.descripcion}</Dato>
      <Dato etiqueta={t('prestador.campos.telefono')}>{servicio.contacto?.telefono}</Dato>
      <Dato etiqueta={t('prestador.campos.correo')}>{servicio.contacto?.correo}</Dato>
      <Dato etiqueta={t('prestador.campos.direccion')}>{servicio.direccion}</Dato>
      <Dato etiqueta={t('prestador.ubicacion.titulo')}>
        {servicio.coordenadas &&
          t('prestador.detalle.coordenadas', {
            latitud: servicio.coordenadas.latitud,
            longitud: servicio.coordenadas.longitud,
          })}
      </Dato>
      <Dato etiqueta={t('prestador.campos.horarioAtencion')}>{servicio.horarioAtencion}</Dato>
      <Dato etiqueta={t('prestador.campos.precioBase')}>{dinero(servicio.precioBase)}</Dato>
      <Dato etiqueta={t('prestador.registro.secciones.accesibilidad')}>
        {servicio.condicionesAccesibilidad.map((condicion) => t(`catalogo.accesibilidad.${condicion}`)).join(', ')}
      </Dato>
      {servicio.categoria === 'Tour' && (
        <>
          <Dato etiqueta={t('prestador.campos.puntoEncuentro')}>{comoTexto(detalle.puntoEncuentro)}</Dato>
          <Dato etiqueta={t('prestador.campos.duracionHoras')}>{comoTexto(detalle.duracionHoras)}</Dato>
          <Dato etiqueta={t('prestador.campos.nivelDificultad')}>{comoTexto(detalle.nivelDificultad)}</Dato>
          <Dato etiqueta={t('prestador.campos.aforoMaxGrupo')}>{comoTexto(detalle.aforoMaxGrupo)}</Dato>
        </>
      )}
      {servicio.categoria === 'Gastronomia' && (
        <>
          <Dato etiqueta={t('prestador.campos.tipoCocina')}>{comoTexto(detalle.tipoCocina)}</Dato>
          <Dato etiqueta={t('prestador.campos.aforoMaxTurno')}>{comoTexto(detalle.aforoMaxTurno)}</Dato>
          <Dato etiqueta={t('prestador.campos.rangoPrecioPlato')}>{comoTexto(detalle.rangoPrecioPlato)}</Dato>
          <Dato etiqueta={t('prestador.campos.menuVegetariano')}>
            {detalle.menuVegetariano === true ? t('prestador.detalle.si') : t('prestador.detalle.no')}
          </Dato>
        </>
      )}
      {servicio.categoria === 'AlquilerEquipo' && (
        <>
          <Dato etiqueta={t('prestador.campos.tipoVehiculoEquipo')}>{comoTexto(detalle.tipoVehiculoEquipo)}</Dato>
          <Dato etiqueta={t('prestador.campos.unidadesDisponibles')}>{comoTexto(detalle.unidadesDisponibles)}</Dato>
          <Dato etiqueta={t('prestador.campos.condicionesAlquiler')}>{comoTexto(detalle.condicionesAlquiler)}</Dato>
        </>
      )}
      {servicio.categoria === 'Alojamiento' && (
        <>
          <Dato etiqueta={t('prestador.campos.serviciosIncluidos')}>
            {(Array.isArray(detalle.serviciosIncluidos) ? (detalle.serviciosIncluidos as string[]) : [])
              .map((valor) => t(`catalogo.serviciosIncluidos.${valor}`, { defaultValue: valor }))
              .join(', ')}
          </Dato>
          <Dato etiqueta={t('prestador.registro.unidades')}>
            {unidades.length > 0 && (
              <ul className="prestador__lista-unidades">
                {unidades.map((unidad, indice) => (
                  <li key={comoTexto(unidad.idUnidad) || indice}>
                    {t('prestador.detalle.unidad', {
                      nombre: comoTexto(unidad.nombre),
                      tipo: comoTexto(unidad.tipoHabitacion),
                      habitaciones: comoTexto(unidad.numHabitaciones),
                      huespedes: comoTexto(unidad.capacidadMaxHuespedes),
                      precio: dinero(unidad.precioNoche),
                    })}
                  </li>
                ))}
              </ul>
            )}
          </Dato>
        </>
      )}
      {servicio.oferta && (
        <>
          <Dato etiqueta={t('prestador.registro.diasOperacion')}>
            {dias.map((dia) => t(`comun.dias.${dia}`, { defaultValue: dia })).join(', ')}
          </Dato>
          {servicio.categoria === 'Tour' && <Dato etiqueta={t('prestador.registro.salidas')}>{salidas.join(', ')}</Dato>}
        </>
      )}
    </dl>
  )
}

/**
 * Detalle propio de WF-18 (DEC-169): estado, fecha de revisión y motivo del rechazo, más los datos tal como se
 * enviaron. «Editar y reenviar» sigue deshabilitado (DEC-22). Después del 201 llega con el aviso «Enviado a
 * aprobación» (DEC-168). Un servicio ajeno o inexistente responde 404 y se muestra como no encontrado.
 */
export function DetalleMiServicioPage() {
  const { t, i18n } = useTranslation()
  const { id = '' } = useParams()
  const ubicacion = useLocation()
  const enviado = (ubicacion.state as { enviado?: boolean } | null)?.enviado === true
  const servicio = useMiServicio(id)

  if (servicio.estado === 'error' && servicio.error === 'sinPermiso') return <SinPermiso />

  return (
    <div className="prestador">
      <p className="prestador__volver">
        <Link to="/prestador/servicios">
          <span aria-hidden="true">← </span>
          {t('prestador.detalle.volver')}
        </Link>
      </p>
      {servicio.estado === 'cargando' && (
        <p className="estado-carga" role="status" aria-busy="true">
          {t('prestador.detalle.cargando')}
        </p>
      )}
      {servicio.estado === 'error' && (
        <div className="prestador__error-general" role="alert">
          <p>{t(`prestador.detalle.errores.${servicio.error}`)}</p>
          {servicio.error !== 'noEncontrado' && (
            <button type="button" className="boton-secundario" onClick={servicio.recargar}>
              {t('catalogo.busqueda.reintentar')}
            </button>
          )}
        </div>
      )}
      {servicio.estado === 'ok' && (
        <article className="tarjeta prestador__seccion" aria-labelledby="detalle-mi-servicio">
          {enviado && (
            <p className="prestador__aviso prestador__aviso--exito" role="status">
              {t('prestador.detalle.enviado')}
            </p>
          )}
          <h1 id="detalle-mi-servicio">{servicio.datos.nombreComercial}</h1>
          <dl className="prestador__datos prestador__datos--estado">
            <div>
              <dt>{t('prestador.misServicios.columnas.estado')}</dt>
              <dd>
                <InsigniaEstadoServicio estado={servicio.datos.estadoPublicacion} />
              </dd>
            </div>
            <div>
              <dt>{t('prestador.misServicios.columnas.registrado')}</dt>
              <dd>{formatearDia(servicio.datos.fechaRegistro, i18n.language)}</dd>
            </div>
            {servicio.datos.fechaRevision && (
              <div>
                <dt>{t('prestador.detalle.revisado')}</dt>
                <dd>{formatearDia(servicio.datos.fechaRevision, i18n.language)}</dd>
              </div>
            )}
          </dl>
          {servicio.datos.estadoPublicacion === 'Rechazado' && servicio.datos.motivoRechazo && (
            <section className="prestador__rechazo" aria-labelledby="motivo-rechazo">
              <h2 id="motivo-rechazo">{t('prestador.detalle.motivoRechazo')}</h2>
              <p>{servicio.datos.motivoRechazo}</p>
            </section>
          )}
          <h2>{t('prestador.detalle.datosEnviados')}</h2>
          <DatosEnviados servicio={servicio.datos} />
          <div className="prestador__acciones">
            <Link className="boton-secundario" to="/prestador/servicios">
              {t('prestador.detalle.volver')}
            </Link>
            {servicio.datos.estadoPublicacion === 'Publicado' && (
              <Link className="boton-secundario" to={`/servicios/${encodeURIComponent(servicio.datos.id)}`}>
                {t('prestador.misServicios.verFicha')}
              </Link>
            )}
            <BotonNoDisponible>{t('prestador.detalle.editar')}</BotonNoDisponible>
          </div>
        </article>
      )}
    </div>
  )
}
