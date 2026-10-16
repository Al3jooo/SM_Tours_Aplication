import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { formatearCentavos } from '../dinero'
import { formatearFecha, horaCorta } from '../fechasBogota'

const FECHA_LARGA: Intl.DateTimeFormatOptions = { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' }

export interface DatosResumen {
  servicio: string
  categoria: 'Alojamiento' | 'Tour'
  unidad?: string
  entrada?: string
  salida?: string
  noches?: number
  habitaciones?: number
  fecha?: string
  hora?: string
  personas?: number
  /** Centavos; null si aún no hay unidad u horario, o si el precio publicado no tiene un formato válido. */
  unitario: number | null
  total: number | null
  /** El turista aceptó un precio nuevo (PRECIO_CAMBIADO). */
  precioActualizado: boolean
}

/**
 * Panel «Resumen de reserva» de WF-10 (RF37): lo que se va a reservar y el total ANTES de confirmar, con el aviso de
 * cancelación (RF44) y la información que envía ms-booking (check-in, check-out y plazo). No muestra lo que el
 * prototipo no tiene (impuestos, pagos, RNT): son pendientes de la trazabilidad.
 */
export function ResumenReserva({
  datos,
  horario,
  motivoNoConfirmable,
  enviando,
  onConfirmar,
}: {
  datos: DatosResumen
  /** De /disponibilidad; null mientras no se ha consultado. */
  horario: { horaCheckIn: string; horaCheckOut: string; plazoCancelacionHoras: number } | null
  motivoNoConfirmable: string | null
  enviando: boolean
  onConfirmar: () => void
}) {
  const { t, i18n } = useTranslation()
  const id = useId()
  const fecha = (valor: string) => formatearFecha(valor, i18n.language, FECHA_LARGA)
  const bloqueado = motivoNoConfirmable !== null || enviando

  const filas: [string, string][] = [[t('reserva.resumen.servicio'), datos.servicio]]
  if (datos.categoria === 'Alojamiento') {
    if (datos.unidad) filas.push([t('reserva.resumen.habitacion'), datos.unidad])
    if (datos.entrada && datos.salida) {
      filas.push([t('reserva.resumen.fechas'), `${fecha(datos.entrada)} → ${fecha(datos.salida)}`])
    }
    if (datos.noches) filas.push([t('reserva.resumen.noches'), String(datos.noches)])
    if (datos.habitaciones) filas.push([t('reserva.resumen.habitaciones'), String(datos.habitaciones)])
    if (datos.personas) filas.push([t('reserva.resumen.huespedes'), String(datos.personas)])
  } else {
    if (datos.fecha) filas.push([t('reserva.resumen.fecha'), fecha(datos.fecha)])
    if (datos.hora) filas.push([t('reserva.resumen.hora'), datos.hora])
    if (datos.personas) filas.push([t('reserva.resumen.personas'), String(datos.personas)])
  }

  const linea =
    datos.unitario === null
      ? null
      : datos.categoria === 'Alojamiento'
        ? datos.noches && datos.habitaciones
          ? `${formatearCentavos(datos.unitario, i18n.language)} × ${t('reserva.resumen.nNoches', { count: datos.noches })} × ${t('reserva.resumen.nHabitaciones', { count: datos.habitaciones })}`
          : null
        : datos.personas
          ? `${formatearCentavos(datos.unitario, i18n.language)} × ${t('reserva.resumen.nPersonas', { count: datos.personas })}`
          : null

  return (
    <aside className="resumen-reserva" aria-labelledby={`${id}-titulo`}>
      <h2 id={`${id}-titulo`}>{t('reserva.resumen.titulo')}</h2>
      <dl className="resumen-reserva__datos">
        {filas.map(([etiqueta, valor]) => (
          <div key={etiqueta}>
            <dt>{etiqueta}</dt>
            <dd>{valor}</dd>
          </div>
        ))}
      </dl>
      {linea && <p className="resumen-reserva__linea">{linea}</p>}
      {datos.total !== null && (
        <p className="resumen-reserva__total">
          <span>{datos.precioActualizado ? t('reserva.resumen.totalActualizado') : t('reserva.resumen.total')}</span>
          <strong>{formatearCentavos(datos.total, i18n.language)}</strong>
        </p>
      )}
      {horario && (
        <p className="resumen-reserva__aviso">
          {t('reserva.resumen.cancelacionGratuita', { count: horario.plazoCancelacionHoras })}
        </p>
      )}
      <button
        type="button"
        className={`boton-principal resumen-reserva__confirmar${bloqueado ? ' deshabilitado' : ''}`}
        aria-disabled={bloqueado ? true : undefined}
        aria-describedby={motivoNoConfirmable ? `${id}-motivo` : undefined}
        aria-busy={enviando ? true : undefined}
        onClick={() => {
          if (!bloqueado) onConfirmar()
        }}
      >
        {enviando ? t('reserva.confirmando') : t('reserva.confirmar')}
      </button>
      {motivoNoConfirmable && (
        <p id={`${id}-motivo`} className="resumen-reserva__motivo">
          {motivoNoConfirmable}
        </p>
      )}
      {horario && (
        <section className="resumen-reserva__info" aria-labelledby={`${id}-info`}>
          <h3 id={`${id}-info`}>{t('reserva.informacion.titulo')}</h3>
          <ul>
            {datos.categoria === 'Alojamiento' && (
              <>
                <li>{t('reserva.informacion.checkIn', { hora: horaCorta(horario.horaCheckIn) })}</li>
                <li>{t('reserva.informacion.checkOut', { hora: horaCorta(horario.horaCheckOut) })}</li>
              </>
            )}
            <li>{t('reserva.informacion.plazo', { count: horario.plazoCancelacionHoras })}</li>
          </ul>
        </section>
      )}
    </aside>
  )
}
