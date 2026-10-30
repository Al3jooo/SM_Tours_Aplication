import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { IconoCalendario } from '../../../shared/components/Iconos'
import { etiquetaCategoria } from '../../catalogo/componentes/etiquetas'
import { InsigniaReserva } from '../../reservas/componentes/InsigniaReserva'
import { centavos, formatearCentavos } from '../../reservas/dinero'
import { diasEntre, formatearFecha, formatearInstante, horaCorta } from '../../reservas/fechasBogota'
import type { ReservaPrestador } from '../reservasPrestadorApi'

/**
 * Una reserva del historial del Prestador (RF12, DEC-204), con el lenguaje visual de WF-11: servicio y categoría, unidad
 * o salida, fechas, cantidades, precio unitario y total, estado, y las fechas de creación y de cancelación. **Ningún
 * dato del Turista** (el contrato no los trae, Ley 1581) y sin acciones: el Prestador no cancela.
 */
export function TarjetaReservaPrestador({ reserva }: { reserva: ReservaPrestador }) {
  const { t, i18n } = useTranslation()
  const id = useId()
  const dinero = (valor: number | string) => {
    const enCentavos = centavos(valor)
    return enCentavos === null ? '—' : formatearCentavos(enCentavos, i18n.language)
  }

  const fechas =
    reserva.categoria === 'Alojamiento' && reserva.fechaSalida
      ? t('misReservas.tarjeta.estadia', {
          entrada: formatearFecha(reserva.fechaEntrada, i18n.language),
          salida: formatearFecha(reserva.fechaSalida, i18n.language),
          noches: t('reserva.resumen.nNoches', { count: diasEntre(reserva.fechaEntrada, reserva.fechaSalida) }),
        })
      : t('misReservas.tarjeta.tour', {
          fecha: formatearFecha(reserva.fechaEntrada, i18n.language),
          hora: reserva.horaInicio ? horaCorta(reserva.horaInicio) : '',
        })
  const cantidades =
    reserva.categoria === 'Alojamiento'
      ? `${t('reserva.resumen.nHabitaciones', { count: reserva.cantidad })} · ${t('misReservas.tarjeta.huespedes', { count: reserva.personas })}`
      : t('reserva.resumen.nPersonas', { count: reserva.personas })
  const unitario = t(reserva.unidadCobro === 'Noche' ? 'prestador.historial.porNoche' : 'prestador.historial.porPersona', {
    precio: dinero(reserva.precioUnitario),
  })

  return (
    <article className={`tarjeta-reserva tarjeta-reserva--${reserva.estado}`} aria-labelledby={`${id}-titulo`}>
      <InsigniaReserva estado={reserva.estado} />
      <h3 id={`${id}-titulo`} className="tarjeta-reserva__titulo">
        {reserva.nombreServicio}
      </h3>
      <p className="tarjeta-reserva__unidad">
        {etiquetaCategoria(t, reserva.categoria)}
        {reserva.nombreUnidad && ` · ${reserva.nombreUnidad}`}
      </p>
      <dl className="tarjeta-reserva__datos">
        <div>
          <dt>
            <IconoCalendario />
            <span className="solo-lector">{t('misReservas.tarjeta.fechas')}</span>
          </dt>
          <dd>{fechas}</dd>
        </div>
        <div>
          <dt className="solo-lector">{t('misReservas.tarjeta.cantidades')}</dt>
          <dd>{cantidades}</dd>
        </div>
        <div>
          <dt>{t('prestador.historial.precioUnitario')}</dt>
          <dd>{unitario}</dd>
        </div>
        <div>
          <dt>{t('prestador.historial.reservadaEl')}</dt>
          <dd>{formatearInstante(reserva.fechaCreacion, i18n.language)}</dd>
        </div>
        {reserva.fechaCancelacion && (
          <div>
            <dt>{t('prestador.historial.canceladaEl')}</dt>
            <dd>{formatearInstante(reserva.fechaCancelacion, i18n.language)}</dd>
          </div>
        )}
        <div className="tarjeta-reserva__total">
          <dt>{t('reserva.resumen.total')}</dt>
          <dd>{dinero(reserva.precioTotal)}</dd>
        </div>
      </dl>
    </article>
  )
}
