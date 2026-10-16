import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { rutaDetalle } from '../../catalogo/detalle/navegacionDetalle'
import { centavos, formatearCentavos } from '../dinero'
import { formatearFecha, formatearInstante, horaCorta } from '../fechasBogota'
import type { Reserva } from '../reservasApi'

/**
 * Después del 201 (RF20): la reserva tal como quedó guardada en ms-booking (nombres y precio congelados, RF32) y el
 * camino a «Mis reservas». El foco va al título para que el lector de pantalla anuncie la confirmación.
 */
export function ConfirmacionReserva({ reserva }: { reserva: Reserva }) {
  const { t, i18n } = useTranslation()
  const titulo = useRef<HTMLHeadingElement>(null)
  const total = centavos(reserva.precioTotal)

  useEffect(() => {
    titulo.current?.focus()
  }, [])

  const filas: [string, string][] = [[t('reserva.resumen.servicio'), reserva.nombreServicio]]
  if (reserva.nombreUnidad) filas.push([t('reserva.resumen.habitacion'), reserva.nombreUnidad])
  if (reserva.categoria === 'Alojamiento' && reserva.fechaSalida) {
    filas.push([
      t('reserva.resumen.fechas'),
      `${formatearFecha(reserva.fechaEntrada, i18n.language)} → ${formatearFecha(reserva.fechaSalida, i18n.language)}`,
    ])
    filas.push([t('reserva.resumen.habitaciones'), String(reserva.cantidad)])
    filas.push([t('reserva.resumen.huespedes'), String(reserva.personas)])
  } else {
    filas.push([t('reserva.resumen.fecha'), formatearFecha(reserva.fechaEntrada, i18n.language)])
    if (reserva.horaInicio) filas.push([t('reserva.resumen.hora'), horaCorta(reserva.horaInicio)])
    filas.push([t('reserva.resumen.personas'), String(reserva.personas)])
  }
  if (total !== null) filas.push([t('reserva.resumen.total'), formatearCentavos(total, i18n.language)])

  return (
    <section className="confirmacion-reserva" aria-labelledby="titulo-confirmacion">
      <h2 id="titulo-confirmacion" ref={titulo} tabIndex={-1}>
        {t('reserva.confirmacion.titulo')}
      </h2>
      <p>{t('reserva.confirmacion.texto')}</p>
      <dl className="resumen-reserva__datos">
        {filas.map(([etiqueta, valor]) => (
          <div key={etiqueta}>
            <dt>{etiqueta}</dt>
            <dd>{valor}</dd>
          </div>
        ))}
      </dl>
      <p className="confirmacion-reserva__plazo">
        {t('reserva.confirmacion.cancelableHasta', { fecha: formatearInstante(reserva.cancelableHasta, i18n.language) })}
      </p>
      <p className="confirmacion-reserva__acciones">
        <Link className="boton-principal" to="/mis-reservas">
          {t('reserva.confirmacion.verMisReservas')}
        </Link>
        <Link to={rutaDetalle(reserva.idServicio)}>{t('reserva.volverDetalle')}</Link>
      </p>
    </section>
  )
}
