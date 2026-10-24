import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { formatearDia } from '../../../shared/formato'
import type { BusquedaReciente } from '../historialApi'
import { rutaRepetir, textoBusqueda } from '../textoBusqueda'

/** Lo que lleva la navegación al repetir una búsqueda: si hay que volver a pedir la ubicación (DEC-88, DEC-96). */
export interface EstadoRepetir {
  repetirBusqueda: true
  pedirUbicacion: boolean
}

/**
 * «Últimas búsquedas» de WF-05 (RF60, DEC-96): hasta 10, la más reciente primero, solo con la sesión de un turista
 * (quien la monta decide); sin búsquedas no se muestra. Cada una es un enlace a /explorar con sus criterios en la URL
 * (un clic la repite); la fecha y, si usaba la ubicación, que se volverá a pedir, van como descripción accesible.
 */
export function UltimasBusquedas({ elementos }: { elementos: BusquedaReciente[] }) {
  const { t, i18n } = useTranslation()
  const id = useId()
  if (elementos.length === 0) return null
  return (
    <section className="ultimas-busquedas" aria-labelledby={`${id}-titulo`}>
      <h2 id={`${id}-titulo`} className="ultimas-busquedas__titulo">
        {t('historial.titulo')}
      </h2>
      <ul className="ultimas-busquedas__lista">
        {elementos.map((busqueda) => {
          const estado: EstadoRepetir = { repetirBusqueda: true, pedirUbicacion: busqueda.criterios.usarUbicacion }
          return (
            <li key={busqueda.id}>
              <Link
                className="chip ultimas-busquedas__enlace"
                to={rutaRepetir(busqueda.criterios)}
                state={estado}
                aria-describedby={`${id}-${busqueda.id}`}
              >
                {textoBusqueda(busqueda.criterios, t, i18n.language)}
              </Link>
              <span id={`${id}-${busqueda.id}`} className="solo-lector">
                {t('historial.fecha', { fecha: formatearDia(busqueda.fecha, i18n.language) })}
                {busqueda.criterios.usarUbicacion && ` ${t('historial.pideUbicacion')}`}
              </span>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
