import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation, useParams } from 'react-router-dom'
import { centavos, formatearCentavos } from '../../reservas/dinero'
import { CATEGORIAS_RESERVABLES } from '../../reservas/reservasApi'
import { ImagenOferta } from '../componentes/ImagenOferta'
import { etiquetaCategoria } from '../componentes/etiquetas'
import { formatearHoras } from '../formato'
import { useDetalleServicio, type DetalleServicio, type ErrorDetalle } from './detalleApi'
import { leerVolverA, rutaReserva } from './navegacionDetalle'
import '../catalogo.css'
import './detalle.css'

/** Miniaturas de la galería de WF-06 (1 grande + 4). */
const MAXIMO_MINIATURAS = 4

/**
 * WF-06, versión mínima (pública): solo los datos que existen en el catálogo. Calificación, favoritos, afluencia,
 * contacto, «Reportar», horarios y destinos alternativos no se muestran: son pendientes de docs/trazabilidad.md.
 * «Ver disponibilidad y reservar» (WF-10) aparece solo en las categorías que se reservan (Tour y Alojamiento).
 */
export function DetalleServicioPage() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  const ubicacion = useLocation()
  const volverA = leerVolverA(ubicacion.state)
  const detalle = useDetalleServicio(id)

  return (
    <div className="detalle">
      <p className="detalle__volver">
        <Link to={volverA ?? '/explorar'}>
          <span aria-hidden="true">← </span>
          {t('detalle.volverBusqueda')}
        </Link>
      </p>
      {detalle.estado === 'cargando' && (
        <p className="estado-carga" role="status" aria-busy="true">
          {t('detalle.cargando')}
        </p>
      )}
      {detalle.estado === 'error' && <ErrorDetalleServicio error={detalle.error} onReintentar={detalle.recargar} />}
      {detalle.estado === 'ok' && <FichaServicio servicio={detalle.servicio} volverA={volverA} />}
    </div>
  )
}

/** Estados de error del detalle (y de WF-10): 404, servicio caído, red u otro. */
export function ErrorDetalleServicio({ error, onReintentar }: { error: ErrorDetalle; onReintentar: () => void }) {
  const { t } = useTranslation()
  return (
    <div className="estado-catalogo estado-catalogo--error" role="alert">
      <p className="estado-catalogo__mensaje">{t(`detalle.errores.${error}`)}</p>
      {error === 'noDisponible' && <p className="estado-catalogo__ayuda">{t('catalogo.busqueda.noDisponibleAyuda')}</p>}
      {error !== 'noEncontrado' && (
        <button type="button" className="boton-secundario" onClick={onReintentar}>
          {t('catalogo.busqueda.reintentar')}
        </button>
      )}
    </div>
  )
}

function FichaServicio({ servicio, volverA }: { servicio: DetalleServicio; volverA: string | null }) {
  const { t, i18n } = useTranslation()
  const idTitulo = useId()
  const precio = centavos(servicio.precioBase)
  const [portada, ...resto] = servicio.imagenes
  const miniaturas = resto.slice(0, MAXIMO_MINIATURAS)

  return (
    <article className="detalle__ficha" aria-labelledby={idTitulo}>
      <header className="detalle__encabezado">
        <p className="detalle__categoria">{etiquetaCategoria(t, servicio.categoria)}</p>
        <h1 id={idTitulo}>{servicio.nombreComercial}</h1>
        {precio !== null && (
          <p className="detalle__precio">{t('catalogo.tarjeta.desde', { precio: formatearCentavos(precio, i18n.language) })}</p>
        )}
      </header>

      <div className="detalle__galeria">
        <div className="detalle__portada">
          <ImagenOferta resultado={{ imagen: portada ?? null, categoria: servicio.categoria, tipo: 'Servicio' }} />
        </div>
        {miniaturas.length > 0 && (
          <ul className="detalle__miniaturas">
            {miniaturas.map((imagen, indice) => (
              <li key={imagen}>
                <img
                  src={imagen}
                  alt={t('detalle.foto', { numero: indice + 2, nombre: servicio.nombreComercial })}
                  loading="lazy"
                  decoding="async"
                  width={150}
                  height={100}
                />
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="detalle__cuerpo">
        {servicio.descripcion && (
          <section aria-labelledby={`${idTitulo}-descripcion`}>
            <h2 id={`${idTitulo}-descripcion`}>{t('detalle.descripcion')}</h2>
            <p className="detalle__descripcion">{servicio.descripcion}</p>
          </section>
        )}
        <DatosCategoria servicio={servicio} idTitulo={idTitulo} />
      </div>

      {CATEGORIAS_RESERVABLES.includes(servicio.categoria) && (
        <p className="detalle__acciones">
          <Link className="boton-principal" to={rutaReserva(servicio.id)} state={volverA ? { volverA } : undefined}>
            {t('detalle.reservar')}
          </Link>
        </p>
      )}
    </article>
  )
}

function DatosCategoria({ servicio, idTitulo }: { servicio: DetalleServicio; idTitulo: string }) {
  const { t, i18n } = useTranslation()
  const dato = (etiqueta: string, valor: string) => (
    <div key={etiqueta}>
      <dt>{etiqueta}</dt>
      <dd>{valor}</dd>
    </div>
  )

  if (servicio.categoria === 'Alojamiento') {
    const { serviciosIncluidos, unidades } = servicio.detalleCategoria
    return (
      <>
        {serviciosIncluidos.length > 0 && (
          <section aria-labelledby={`${idTitulo}-incluidos`}>
            <h2 id={`${idTitulo}-incluidos`}>{t('detalle.serviciosIncluidos')}</h2>
            <ul className="detalle__etiquetas">
              {serviciosIncluidos.map((incluido) => (
                <li key={incluido}>{t(`catalogo.serviciosIncluidos.${incluido}`, { defaultValue: incluido })}</li>
              ))}
            </ul>
          </section>
        )}
        <section aria-labelledby={`${idTitulo}-unidades`}>
          <h2 id={`${idTitulo}-unidades`}>{t('detalle.unidades')}</h2>
          <ul className="detalle__unidades">
            {unidades.map((unidad) => {
              const precio = centavos(unidad.precioNoche)
              return (
                <li key={unidad.idUnidad} className="detalle__unidad">
                  <h3>{unidad.nombre}</h3>
                  <dl>
                    {dato(t('detalle.tipoHabitacion'), unidad.tipoHabitacion)}
                    {dato(t('detalle.habitaciones'), String(unidad.numHabitaciones))}
                    {dato(t('detalle.capacidad'), t('detalle.huespedesPorHabitacion', { count: unidad.capacidadMaxHuespedes }))}
                  </dl>
                  {precio !== null && (
                    <p className="detalle__unidad-precio">
                      {t('detalle.precioNoche', { precio: formatearCentavos(precio, i18n.language) })}
                    </p>
                  )}
                </li>
              )
            })}
          </ul>
        </section>
      </>
    )
  }

  const datos: [string, string][] = []
  if (servicio.categoria === 'Tour') {
    const tour = servicio.detalleCategoria
    datos.push([t('detalle.duracion'), t('detalle.horas', { horas: formatearHoras(tour.duracionHoras, i18n.language) })])
    datos.push([t('detalle.puntoEncuentro'), tour.puntoEncuentro])
    if (tour.nivelDificultad) datos.push([t('detalle.dificultad'), tour.nivelDificultad])
    datos.push([t('detalle.aforoGrupo'), t('detalle.personas', { count: tour.aforoMaxGrupo })])
  } else if (servicio.categoria === 'Gastronomia') {
    const restaurante = servicio.detalleCategoria
    datos.push([t('detalle.tipoCocina'), restaurante.tipoCocina])
    datos.push([t('detalle.aforoTurno'), t('detalle.personas', { count: restaurante.aforoMaxTurno })])
    if (restaurante.menuVegetariano !== null) {
      datos.push([t('detalle.menuVegetariano'), t(restaurante.menuVegetariano ? 'detalle.si' : 'detalle.no')])
    }
    if (restaurante.rangoPrecioPlato) datos.push([t('detalle.rangoPrecioPlato'), restaurante.rangoPrecioPlato])
  } else {
    const alquiler = servicio.detalleCategoria
    datos.push([t('detalle.tipoEquipo'), alquiler.tipoVehiculoEquipo])
    datos.push([t('detalle.unidadesDisponibles'), String(alquiler.unidadesDisponibles)])
    if (alquiler.condicionesAlquiler) datos.push([t('detalle.condiciones'), alquiler.condicionesAlquiler])
  }

  return (
    <section aria-labelledby={`${idTitulo}-datos`}>
      <h2 id={`${idTitulo}-datos`}>{t('detalle.datos')}</h2>
      <dl className="detalle__datos">{datos.map(([etiqueta, valor]) => dato(etiqueta, valor))}</dl>
    </section>
  )
}
