import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { SinPermiso } from '../../shared/components/SinPermiso'
import { actualizarDatosComerciales } from './serviciosPrestadorApi'
import {
  RUTAS_DATOS_COMERCIALES,
  borradorDatosDe,
  cuerpoDatosComerciales,
  esEditable,
  useEdicionServicio,
  validarDatosComerciales,
  valoresDatosComerciales,
  type BorradorDatosComerciales,
} from './edicionServicio'
import { CampoServicio } from './componentes/CampoServicio'
import { textoErrorServicio } from './rutasCampos'
import './prestador.css'

/**
 * WF-21 «Editar datos comerciales» del servicio (RF43, CU «Actualizar datos comerciales», flujo B; DEC-196, DEC-199 a
 * DEC-201), solo para el Prestador, desde el detalle propio de WF-18: el nombre comercial (el «nombre del
 * establecimiento») y el contacto que ven los viajeros en la ficha. En `Publicado` el cambio se ve de inmediato, sin
 * nueva aprobación (DEC-189). Una nota distingue este contacto del correo corporativo del perfil y del correo de acceso
 * (C2).
 */
export function DatosComercialesPage() {
  const { t } = useTranslation()
  const id = useId()
  const { id: idServicio = '' } = useParams()
  const edicion = useEdicionServicio(idServicio, {
    borradorDe: borradorDatosDe,
    validar: (_servicio, borrador: BorradorDatosComerciales) => validarDatosComerciales(borrador),
    rutas: () => [...RUTAS_DATOS_COMERCIALES],
    cuerpo: (_servicio, borrador) => cuerpoDatosComerciales(borrador),
    enviar: actualizarDatosComerciales,
    valores: valoresDatosComerciales,
    anuncioExito: 'prestador.datosComerciales.guardado',
  })
  const { carga, servicio, borrador } = edicion

  if (carga.estado === 'error' && carga.error === 'sinPermiso') return <SinPermiso />

  const error = (ruta: string) => textoErrorServicio(t, ruta, edicion.errores[ruta])
  const valorActual = (ruta: string) =>
    ruta in edicion.actuales ? t('prestador.edicion.valorActual', { valor: edicion.actuales[ruta] || '—' }) : undefined
  const cambiar = (campo: keyof BorradorDatosComerciales, ruta: string) => (valor: string) =>
    edicion.cambiar((actual) => ({ ...actual, [campo]: valor }), [ruta, 'contacto'])
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
          <li aria-current="page">{t('prestador.datosComerciales.titulo')}</li>
        </ol>
      </nav>
      <h1>
        {servicio
          ? t('prestador.datosComerciales.tituloDe', { nombre: servicio.nombreComercial })
          : t('prestador.datosComerciales.titulo')}
      </h1>

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
          <p className="prestador__apoyo">{t('prestador.datosComerciales.apoyo')}</p>
          <p className="prestador__aviso">
            {t(servicio.estadoPublicacion === 'Publicado' ? 'prestador.edicion.avisoPublicado' : 'prestador.edicion.avisoPendiente')}
          </p>
          <form className="prestador__formulario" noValidate onSubmit={(evento) => void edicion.guardar(evento)} aria-labelledby={`${id}-datos`}>
            <section className="tarjeta prestador__seccion" aria-labelledby={`${id}-datos`}>
              <h2 id={`${id}-datos`}>{t('prestador.datosComerciales.seccion')}</h2>
              <CampoServicio ruta="nombreComercial" etiqueta={t('prestador.campos.nombreComercial')}
                ayuda={valorActual('nombreComercial')} valor={borrador.nombreComercial}
                onCambiar={cambiar('nombreComercial', 'nombreComercial')} error={error('nombreComercial')} />
              <p className="prestador__nota">{t('prestador.datosComerciales.notaContactos')}</p>
              <div className="prestador__columnas">
                <CampoServicio ruta="contacto.telefono" etiqueta={t('prestador.campos.telefono')} type="tel"
                  autoComplete="tel" ayuda={valorActual('contacto.telefono')} valor={borrador.telefono}
                  onCambiar={cambiar('telefono', 'contacto.telefono')} error={error('contacto.telefono')} />
                <CampoServicio ruta="contacto.correo" etiqueta={t('prestador.campos.correo')} type="email"
                  autoComplete="email" ayuda={valorActual('contacto.correo')} valor={borrador.correo}
                  onCambiar={cambiar('correo', 'contacto.correo')} error={error('contacto.correo')} />
              </div>
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
