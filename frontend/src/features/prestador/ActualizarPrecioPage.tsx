import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { SinPermiso } from '../../shared/components/SinPermiso'
import { etiquetaCategoria } from '../catalogo/componentes/etiquetas'
import { centavos, formatearCentavos } from '../reservas/dinero'
import { actualizarPrecio } from './serviciosPrestadorApi'
import {
  borradorPrecioDe,
  cuerpoPrecio,
  esEditable,
  fusionarPrecio,
  precioDesdeUnidades,
  rutasPrecio,
  useEdicionServicio,
  validarPrecio,
  valoresPrecio,
} from './edicionServicio'
import { CampoServicio } from './componentes/CampoServicio'
import { PreciosUnidades } from './componentes/PreciosUnidades'
import { textoErrorServicio } from './rutasCampos'
import './prestador.css'

/**
 * WF-20 «Actualizar precio» (RF32, CU «Actualizar tarifas»; DEC-196, DEC-198, DEC-200, DEC-201), solo para el
 * Prestador, desde el detalle propio de WF-18. Tour, Gastronomía y Alquiler editan el precio «desde»; Alojamiento, el
 * precio por noche de cada unidad, con el «desde» derivado en un `<output>` (DEC-127, DEC-163) y todas las unidades en
 * el cuerpo. La oferta no se edita (DEC-183) y no hace falta una nueva aprobación (DEC-185).
 */
export function ActualizarPrecioPage() {
  const { t, i18n } = useTranslation()
  const id = useId()
  const { id: idServicio = '' } = useParams()
  const dinero = (valor: number | null) => {
    const enCentavos = centavos(valor)
    return enCentavos === null ? '—' : formatearCentavos(enCentavos, i18n.language)
  }
  const edicion = useEdicionServicio(idServicio, {
    borradorDe: borradorPrecioDe,
    fusionar: fusionarPrecio,
    validar: validarPrecio,
    rutas: rutasPrecio,
    cuerpo: cuerpoPrecio,
    enviar: actualizarPrecio,
    valores: valoresPrecio,
    anuncioExito: 'prestador.precio.guardado',
  })
  const { carga, servicio, borrador } = edicion

  if (carga.estado === 'error' && carga.error === 'sinPermiso') return <SinPermiso />

  const error = (ruta: string) => textoErrorServicio(t, ruta, edicion.errores[ruta])
  const valorActual = (clave: string) =>
    clave in edicion.actuales ? t('prestador.edicion.valorActual', { valor: dinero(edicion.actuales[clave]) }) : undefined
  const rutaDetalle = `/prestador/servicios/${encodeURIComponent(idServicio)}`

  return (
    <div className="prestador">
      <nav aria-label={t('prestador.migas')} className="prestador__migas">
        <ol>
          <li>
            <Link to="/prestador/servicios">{t('prestador.misServicios.titulo')}</Link>
          </li>
          <li>
            <Link to={rutaDetalle}>{servicio?.nombreComercial ?? t('prestador.edicion.servicio')}</Link>
          </li>
          <li aria-current="page">{t('prestador.precio.titulo')}</li>
        </ol>
      </nav>
      <h1>{servicio ? t('prestador.precio.tituloDe', { nombre: servicio.nombreComercial }) : t('prestador.precio.titulo')}</h1>

      {carga.estado === 'cargando' && (
        <p className="estado-carga" role="status" aria-busy="true">
          {t('prestador.detalle.cargando')}
        </p>
      )}
      {(edicion.perdido || (carga.estado === 'error' && carga.error === 'noEncontrado')) && (
        <div className="prestador__error-general" role="alert">
          <p>{t('prestador.detalle.errores.noEncontrado')}</p>
          <Link className="boton-secundario" to="/prestador/servicios">
            {t('prestador.detalle.volver')}
          </Link>
        </div>
      )}
      {carga.estado === 'error' && carga.error !== 'noEncontrado' && (
        <div className="prestador__error-general" role="alert">
          <p>{t(`prestador.detalle.errores.${carga.error}`)}</p>
          <button type="button" className="boton-secundario" onClick={edicion.recargar}>
            {t('catalogo.busqueda.reintentar')}
          </button>
        </div>
      )}
      {!edicion.perdido && servicio && !esEditable(servicio.estadoPublicacion) && (
        <div className="prestador__error-general" role="alert">
          <p>{t('prestador.edicion.noEditable', { estado: t(`prestador.estados.${servicio.estadoPublicacion}`) })}</p>
          <Link className="boton-secundario" to={rutaDetalle}>
            {t('prestador.edicion.volverDetalle')}
          </Link>
        </div>
      )}
      {!edicion.perdido && servicio && borrador && esEditable(servicio.estadoPublicacion) && (
        <>
          <p className="prestador__apoyo">
            {t('prestador.precio.apoyo', { categoria: etiquetaCategoria(t, servicio.categoria) })}
          </p>
          <p className="prestador__aviso">
            {t(servicio.estadoPublicacion === 'Publicado' ? 'prestador.edicion.avisoPublicado' : 'prestador.edicion.avisoPendiente')}
          </p>
          <form className="prestador__formulario" noValidate onSubmit={(evento) => void edicion.guardar(evento)} aria-labelledby={`${id}-precio`}>
            <section className="tarjeta prestador__seccion" aria-labelledby={`${id}-precio`}>
              <h2 id={`${id}-precio`}>{t('prestador.registro.secciones.precio')}</h2>
              {servicio.categoria === 'Alojamiento' ? (
                <>
                  <PreciosUnidades
                    unidades={borrador.unidades}
                    errores={edicion.errores}
                    valorActual={(idUnidad) => valorActual(`unidad:${idUnidad}`)}
                    onCambiar={(indice, valor) =>
                      edicion.cambiar(
                        (actual) => ({
                          ...actual,
                          unidades: actual.unidades.map((unidad, i) => (i === indice ? { ...unidad, precioNoche: valor } : unidad)),
                        }),
                        [`unidades[${indice}].precioNoche`, 'unidades'],
                      )
                    }
                  />
                  <div className="campo">
                    <span className="prestador__rnt-etiqueta" id={`${id}-desde`}>
                      {t('prestador.campos.precioBase')}
                    </span>
                    <output aria-labelledby={`${id}-desde`} className="prestador__desde">
                      {dinero(precioDesdeUnidades(borrador.unidades))}
                    </output>
                    <p className="campo__ayuda">{t('prestador.registro.precioDerivado')}</p>
                  </div>
                </>
              ) : (
                <CampoServicio
                  ruta="precioBase"
                  etiqueta={t('prestador.campos.precioBase')}
                  inputMode="decimal"
                  sufijo="COP"
                  ayuda={valorActual('precioBase')}
                  valor={borrador.precioBase}
                  onCambiar={(valor) => edicion.cambiar((actual) => ({ ...actual, precioBase: valor }), ['precioBase'])}
                  error={error('precioBase')}
                />
              )}
              <p className="campo__ayuda">{t('prestador.precio.ofertaInmutable')}</p>
              <p className="campo__ayuda">{t('prestador.precio.reservasConservan')}</p>
            </section>

            <div ref={edicion.mensajeGeneral} tabIndex={-1} className={edicion.general ? 'prestador__error-general' : undefined} role="alert">
              {edicion.general && (
                <>
                  <p>{t(`prestador.edicion.errores.${edicion.general}`)}</p>
                  {edicion.conRecarga && (
                    <button type="button" className="boton-secundario" onClick={edicion.recargar}>
                      {t('prestador.edicion.recargar')}
                    </button>
                  )}
                </>
              )}
            </div>

            <div className="prestador__acciones">
              <button type="submit" className="boton-principal" disabled={edicion.enviando} aria-busy={edicion.enviando || undefined}>
                {edicion.enviando ? t('prestador.edicion.guardando') : t('prestador.edicion.guardar')}
              </button>
              <button type="button" className="boton-secundario" onClick={edicion.descartar} disabled={edicion.enviando}>
                {t('prestador.edicion.descartar')}
              </button>
              <Link className="boton-secundario" to={rutaDetalle}>
                {t('prestador.registro.cancelar')}
              </Link>
            </div>
          </form>
        </>
      )}
    </div>
  )
}
