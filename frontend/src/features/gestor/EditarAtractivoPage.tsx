import { useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { SinPermiso } from '../../shared/components/SinPermiso'
import { InsigniaEstadoOperativo } from '../catalogo/componentes/InsigniaEstadoOperativo'
import { rutaDetalleAtractivo } from '../catalogo/detalle/navegacionDetalle'
import { DialogoEstadoAtractivo } from './componentes/DialogoEstadoAtractivo'
import { FormularioAtractivo } from './componentes/FormularioAtractivo'
import type { EstadoAnuncioGestor } from './criteriosAtractivosGestor'
import { useEdicionAtractivo } from './useEdicionAtractivo'
import '../prestador/prestador.css'
import './gestor.css'

/**
 * WF-09 en modo edición (CU «Actualizar atractivo»; RF07, RF24, RF27, RF58), solo para el Gestor, que edita cualquier
 * atractivo (DEC-206). Los mismos campos del alta, salvo el estado, que cambia con su diálogo (CU «Cambiar estado
 * operativo», DEC-225), y las imágenes, que se ven en solo lectura: el contrato no permite agregarlas ni quitarlas
 * (DEC-226, pendiente). La `version` no se ve; viaja en cada envío (DEC-207).
 */
export function EditarAtractivoPage() {
  const { t } = useTranslation()
  const id = useId()
  const navegar = useNavigate()
  const { id: idAtractivo = '' } = useParams()
  const edicion = useEdicionAtractivo(idAtractivo)
  const { carga, atractivo, borrador } = edicion
  const [cambiandoEstado, setCambiandoEstado] = useState(false)
  const botonEstado = useRef<HTMLButtonElement>(null)

  if (carga.estado === 'error' && carga.error === 'sinPermiso') return <SinPermiso />

  const valorActual = (ruta: string) =>
    ruta in edicion.actuales ? t('gestor.edicion.valorActual', { valor: edicion.actuales[ruta] || '—' }) : undefined
  const avisosActuales = Object.keys(edicion.actuales).map((ruta) => valorActual(ruta)).filter(Boolean)
  const noEncontrado = edicion.perdido || (carga.estado === 'error' && carga.error === 'noEncontrado')
  const alNoEncontrado = () => {
    const estado: EstadoAnuncioGestor = { anuncio: 'noEncontrado' }
    navegar('/gestor/atractivos', { state: estado })
  }

  return (
    <div className="prestador gestor">
      <nav aria-label={t('gestor.migas')} className="prestador__migas">
        <ol>
          <li>
            <Link to="/gestor/atractivos">{t('gestor.panel.titulo')}</Link>
          </li>
          <li aria-current="page">{atractivo?.nombre ?? t('gestor.edicion.titulo')}</li>
        </ol>
      </nav>
      <h1>{atractivo ? t('gestor.edicion.tituloDe', { nombre: atractivo.nombre }) : t('gestor.edicion.titulo')}</h1>

      {carga.estado === 'cargando' && (
        <p className="estado-carga" role="status" aria-busy="true">
          {t('gestor.edicion.cargando')}
        </p>
      )}
      {noEncontrado && (
        <div className="prestador__error-general" role="alert">
          <p>{t('gestor.edicion.noEncontrado')}</p>
          <Link className="boton-secundario" to="/gestor/atractivos">
            {t('gestor.edicion.volver')}
          </Link>
        </div>
      )}
      {carga.estado === 'error' && carga.error !== 'noEncontrado' && (
        <div className="prestador__error-general" role="alert">
          <p>{t(`gestor.panel.errores.${carga.error}`)}</p>
          <button type="button" className="boton-secundario" onClick={edicion.recargar}>
            {t('catalogo.busqueda.reintentar')}
          </button>
        </div>
      )}

      {!noEncontrado && atractivo && borrador && (
        <>
          <div className="gestor__estado">
            {atractivo.estadoOperativo && <InsigniaEstadoOperativo estado={atractivo.estadoOperativo} />}
            <button ref={botonEstado} type="button" className="boton-secundario" onClick={() => setCambiandoEstado(true)}>
              {t('gestor.panel.cambiarEstado')}
            </button>
            <Link to={rutaDetalleAtractivo(atractivo.id)}>{t('gestor.panel.verFicha')}</Link>
          </div>

          <form className="prestador__formulario" noValidate onSubmit={(evento) => void edicion.guardar(evento)} aria-labelledby={`${id}-titulo`}>
            <span id={`${id}-titulo`} hidden>
              {t('gestor.edicion.titulo')}
            </span>
            {avisosActuales.length > 0 && (
              <ul className="gestor__actuales" aria-label={t('gestor.edicion.cambiosDeOtro')}>
                {Object.keys(edicion.actuales).map((ruta) => (
                  <li key={ruta}>
                    {/* Las claves de i18n no llevan puntos: «coordenadas.latitud» → «latitud». */}
                    {t(`gestor.edicion.rutas.${ruta.replace('coordenadas.', '')}`, { defaultValue: ruta })}: {valorActual(ruta)}
                  </li>
                ))}
              </ul>
            )}
            <FormularioAtractivo
              borrador={borrador}
              errores={edicion.errores}
              conEstado={false}
              onCambiar={edicion.cambiar}
              fotos={
                <section
                  className="tarjeta prestador__seccion"
                  role="group"
                  aria-labelledby={`${id}-fotos`}
                >
                  <h2 id={`${id}-fotos`}>{t('gestor.imagenes.titulo')}</h2>
                  {atractivo.imagenes.length > 0 ? (
                    <ul className="gestor-imagenes__lista">
                      {atractivo.imagenes.map((imagen, indice) => (
                        <li key={imagen} className="gestor-imagenes__item">
                          <img
                            src={imagen}
                            alt={t('gestor.imagenes.foto', { numero: indice + 1, nombre: atractivo.nombre })}
                            width={120}
                            height={80}
                            loading="lazy"
                          />
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p>{t('gestor.imagenes.sinImagenes')}</p>
                  )}
                  <p className="prestador__no-disponible" role="group" aria-disabled="true">
                    <span>{t('gestor.imagenes.editar')}</span>
                    <span className="deshabilitado">
                      <span aria-hidden="true">—</span>
                      <span className="solo-lector">{t('comun.noDisponible')}</span>
                    </span>
                  </p>
                </section>
              }
            />

            <div ref={edicion.mensajeGeneral} tabIndex={-1} className={edicion.general ? 'prestador__error-general' : undefined} role="alert">
              {edicion.general && (
                <>
                  <p>{t(`gestor.edicion.errores.${edicion.general}`)}</p>
                  {edicion.conRecarga && (
                    <button type="button" className="boton-secundario" onClick={edicion.recargar}>
                      {t('gestor.edicion.recargar')}
                    </button>
                  )}
                </>
              )}
            </div>

            <div className="prestador__acciones">
              <button type="submit" className="boton-principal" disabled={edicion.enviando} aria-busy={edicion.enviando || undefined}>
                {edicion.enviando ? t('gestor.edicion.guardando') : t('gestor.edicion.guardar')}
              </button>
              <button type="button" className="boton-secundario" onClick={edicion.descartar} disabled={edicion.enviando}>
                {t('gestor.edicion.descartar')}
              </button>
              <Link className="boton-secundario" to="/gestor/atractivos">
                {t('gestor.registro.cancelar')}
              </Link>
            </div>
          </form>

          {cambiandoEstado && (
            <DialogoEstadoAtractivo
              atractivo={atractivo}
              onCerrar={() => {
                setCambiandoEstado(false)
                setTimeout(() => botonEstado.current?.focus(), 0)
              }}
              onCambiado={(actualizado) => {
                edicion.alCambiarEstado(actualizado)
                setCambiandoEstado(false)
                setTimeout(() => botonEstado.current?.focus(), 0)
              }}
              onNoEncontrado={alNoEncontrado}
            />
          )}
        </>
      )}
    </div>
  )
}
