import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { ImagenOferta } from '../../catalogo/componentes/ImagenOferta'
import type { UnidadAlojamiento } from '../../catalogo/detalle/detalleApi'
import { cupoUnidad } from '../cupos'
import { centavos, formatearCentavos } from '../dinero'
import { formatearFecha } from '../fechasBogota'
import type { Disponibilidad } from '../reservasApi'

/**
 * Unidades de un alojamiento (WF-10, D1) como un grupo de radios: se reserva una sola. Nombres, tipo, capacidad y
 * precio salen del detalle de ms-catalog; el cupo, de la disponibilidad de ms-booking (el mínimo de las noches). Una
 * unidad sin cupo queda con el radio deshabilitado y el motivo en texto, igual que un horario agotado de un tour.
 */
export function UnidadesAlojamiento({
  unidades,
  serviciosIncluidos,
  imagen,
  disponibilidad,
  noches,
  seleccionada,
  onElegir,
}: {
  unidades: UnidadAlojamiento[]
  serviciosIncluidos: string[]
  imagen: string | null
  disponibilidad: Disponibilidad
  noches: string[]
  seleccionada: string | undefined
  onElegir: (idUnidad: string) => void
}) {
  const { t, i18n } = useTranslation()
  const id = useId()

  return (
    <fieldset className="reserva__grupo reserva__unidades">
      <legend>{t('reserva.elegirHabitacion')}</legend>
      <ul>
        {unidades.map((unidad) => {
          const { cupo, nocheSinCupo } = cupoUnidad(disponibilidad, unidad.idUnidad, noches)
          const precio = centavos(unidad.precioNoche)
          const idRadio = `${id}-${unidad.idUnidad}`
          const elegida = seleccionada === unidad.idUnidad
          return (
            <li
              key={unidad.idUnidad}
              className={`tarjeta-unidad${elegida ? ' tarjeta-unidad--elegida' : ''}${cupo === 0 ? ' tarjeta-unidad--sin-cupo' : ''}`}
            >
              <ImagenOferta resultado={{ imagen, categoria: 'Alojamiento', tipo: 'Servicio' }} />
              <div className="tarjeta-unidad__cuerpo">
                <div className="tarjeta-unidad__eleccion">
                  <input
                    type="radio"
                    id={idRadio}
                    name={`${id}-unidad`}
                    value={unidad.idUnidad}
                    checked={elegida}
                    disabled={cupo === 0}
                    aria-describedby={`${idRadio}-datos ${idRadio}-cupo`}
                    onChange={() => onElegir(unidad.idUnidad)}
                  />
                  <label htmlFor={idRadio}>{unidad.nombre}</label>
                </div>
                <p id={`${idRadio}-datos`} className="tarjeta-unidad__datos">
                  {t('reserva.datosUnidad', { tipo: unidad.tipoHabitacion, count: unidad.capacidadMaxHuespedes })}
                  {precio !== null && (
                    <>
                      {' · '}
                      <strong>{t('reserva.precioNoche', { precio: formatearCentavos(precio, i18n.language) })}</strong>
                    </>
                  )}
                </p>
                {serviciosIncluidos.length > 0 && (
                  <ul className="detalle__etiquetas" aria-label={t('detalle.serviciosIncluidos')}>
                    {serviciosIncluidos.map((incluido) => (
                      <li key={incluido}>{t(`catalogo.serviciosIncluidos.${incluido}`, { defaultValue: incluido })}</li>
                    ))}
                  </ul>
                )}
                <p
                  id={`${idRadio}-cupo`}
                  className={`tarjeta-unidad__cupo${cupo === 0 ? ' tarjeta-unidad__cupo--agotado' : ''}`}
                >
                  {cupo > 0
                    ? t('reserva.quedanHabitaciones', { count: cupo })
                    : nocheSinCupo
                      ? t('reserva.sinCupoNoche', { fecha: formatearFecha(nocheSinCupo, i18n.language) })
                      : t('reserva.agotado')}
                </p>
              </div>
            </li>
          )
        })}
      </ul>
    </fieldset>
  )
}
