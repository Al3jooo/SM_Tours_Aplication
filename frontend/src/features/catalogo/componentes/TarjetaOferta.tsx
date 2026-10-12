import { useId, type ReactNode, type Ref } from 'react'
import { useTranslation } from 'react-i18next'
import { IconoAuditiva, IconoMovilidadReducida, IconoVisual } from '../../../shared/components/Iconos'
import { BotonProximamente } from '../../../shared/components/Proximamente'
import type { CondicionAccesibilidad, ResultadoOferta } from '../catalogoApi'
import { formatearDistancia, formatearPrecio } from '../formato'
import { etiquetaCategoria } from './etiquetas'
import { ImagenOferta } from './ImagenOferta'

const ICONOS_ACCESIBILIDAD: Record<CondicionAccesibilidad, () => ReactNode> = {
  MovilidadReducida: IconoMovilidadReducida,
  Visual: IconoVisual,
  Auditiva: IconoAuditiva,
}

/**
 * Tarjeta de un servicio o atractivo: lista de WF-05 ('lista') y destacados de WF-04 ('destacado').
 * Calificación, afluencia y disponibilidad dependen de datos que aún no existen: se muestran como «—».
 *
 * En la lista, `onResaltar` avisa cuando el puntero o el foco entran o salen (para resaltar su marcador en el mapa);
 * la tarjeta acepta el foco por programa (tabIndex -1) para cuando se pulsa su marcador.
 */
export function TarjetaOferta({
  resultado,
  variante,
  resaltada = false,
  onResaltar,
  ref,
}: {
  resultado: ResultadoOferta
  variante: 'lista' | 'destacado'
  resaltada?: boolean
  onResaltar?: (activa: boolean) => void
  ref?: Ref<HTMLElement>
}) {
  const { t, i18n } = useTranslation()
  const idTitulo = useId()
  const sinDatos = (
    <>
      <span aria-hidden="true">—</span>
      <span className="solo-lector">{t('comun.sinDatos')}</span>
    </>
  )

  return (
    <article
      ref={ref}
      className={`tarjeta-oferta tarjeta-oferta--${variante}${resaltada ? ' tarjeta-oferta--resaltada' : ''}`}
      aria-labelledby={idTitulo}
      tabIndex={onResaltar ? -1 : undefined}
      onMouseEnter={onResaltar && (() => onResaltar(true))}
      onMouseLeave={onResaltar && (() => onResaltar(false))}
      onFocus={onResaltar && (() => onResaltar(true))}
      onBlur={onResaltar && (() => onResaltar(false))}
    >
      <ImagenOferta resultado={resultado} />
      <div className="tarjeta-oferta__cuerpo">
        <p className="tarjeta-oferta__categoria">
          {resultado.tipo === 'Atractivo' && `${t('catalogo.tipos.Atractivo')} · `}
          {etiquetaCategoria(t, resultado.categoria)}
        </p>
        <h3 id={idTitulo} className="tarjeta-oferta__nombre">
          {resultado.nombre}
        </h3>

        {resultado.estadoOperativo && (
          // Decisión 5: los atractivos cerrados o en mantenimiento aparecen con su estado visible (RF33).
          <p className={`insignia-estado insignia-estado--${resultado.estadoOperativo}`}>
            <span className="solo-lector">{t('catalogo.tarjeta.estado')}: </span>
            {t(`catalogo.estados.${resultado.estadoOperativo}`)}
          </p>
        )}

        {variante === 'lista' && resultado.descripcionCorta && (
          <p className="tarjeta-oferta__descripcion">{resultado.descripcionCorta}</p>
        )}

        {resultado.precio !== null && (
          <p className="tarjeta-oferta__precio">
            {t('catalogo.tarjeta.desde', { precio: formatearPrecio(resultado.precio, i18n.language) })}
          </p>
        )}

        <dl className="tarjeta-oferta__datos">
          {resultado.distanciaKm !== null && (
            <div>
              <dt>{t('catalogo.tarjeta.distancia')}</dt>
              <dd>{formatearDistancia(resultado.distanciaKm, i18n.language)}</dd>
            </div>
          )}
          <div>
            <dt>{t('catalogo.tarjeta.calificacion')}</dt>
            <dd>{sinDatos}</dd>
          </div>
          <div>
            {/* Según el SRS: afluencia de los atractivos (RF39) y disponibilidad de cupos de los servicios (RF11). */}
            <dt>{t(resultado.tipo === 'Atractivo' ? 'catalogo.tarjeta.afluencia' : 'catalogo.tarjeta.disponibilidad')}</dt>
            <dd>{sinDatos}</dd>
          </div>
        </dl>

        {resultado.accesibilidad.length > 0 && (
          <ul className="tarjeta-oferta__accesibilidad" aria-label={t('catalogo.tarjeta.accesibilidad')}>
            {resultado.accesibilidad.map((condicion) => {
              const Icono = ICONOS_ACCESIBILIDAD[condicion]
              const etiqueta = t(`catalogo.accesibilidad.${condicion}`)
              return (
                <li key={condicion} title={etiqueta}>
                  <Icono />
                  <span className="solo-lector">{etiqueta}</span>
                </li>
              )
            })}
          </ul>
        )}

        <div className="tarjeta-oferta__acciones">
          <BotonProximamente className="enlace">{t('catalogo.tarjeta.verDetalle')}</BotonProximamente>
          {variante === 'destacado' && (
            <BotonProximamente className="enlace">{t('catalogo.tarjeta.reservar')}</BotonProximamente>
          )}
        </div>
      </div>
    </article>
  )
}
