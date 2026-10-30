import { useTranslation } from 'react-i18next'
import type { UnidadPrecio } from '../edicionServicio'
import { rutaPrecioNoche } from '../edicionServicio'
import { idDeCampo, textoErrorServicio, type ErroresServicio } from '../rutasCampos'
import { CampoServicio, ErrorGrupo } from './CampoServicio'

/**
 * Precio por noche de cada unidad de un Alojamiento (RF32, DEC-198): el nombre y el tipo de habitación en solo lectura
 * (la unidad no se edita aquí, DEC-183) y el precio editable. El id de cada control sale de la ruta del contrato de
 * edición (`unidades[1].precioNoche`), así que el 400 queda junto a su unidad. Las filas no se agregan ni se quitan:
 * el cuerpo lleva todas las unidades, una vez cada una.
 */
export function PreciosUnidades({
  unidades,
  errores,
  valorActual,
  onCambiar,
}: {
  unidades: UnidadPrecio[]
  errores: ErroresServicio
  /** «Valor actual: …» de una unidad después de un 409 SERVICIO_MODIFICADO (DEC-200). */
  valorActual: (idUnidad: string) => string | undefined
  onCambiar: (indice: number, valor: string) => void
}) {
  const { t } = useTranslation()
  const errorGrupo = textoErrorServicio(t, 'unidades', errores.unidades)

  return (
    <fieldset
      id={idDeCampo('unidades')}
      tabIndex={-1}
      className="prestador__grupo"
      aria-describedby={errorGrupo ? `${idDeCampo('unidades')}-error` : undefined}
    >
      <legend>{t('prestador.precio.unidades')}</legend>
      <ul className="prestador__filas">
        {unidades.map((unidad, indice) => {
          const ruta = rutaPrecioNoche(indice)
          return (
            <li key={unidad.idUnidad} className="prestador__unidad prestador__unidad--precio">
              <p className="prestador__unidad-nombre">
                {t('prestador.precio.unidad', { nombre: unidad.nombre, tipo: unidad.tipoHabitacion })}
              </p>
              <CampoServicio
                ruta={ruta}
                etiqueta={t('prestador.precio.precioNocheDe', { nombre: unidad.nombre })}
                inputMode="decimal"
                sufijo="COP"
                ayuda={valorActual(unidad.idUnidad)}
                valor={unidad.precioNoche}
                onCambiar={(valor) => onCambiar(indice, valor)}
                error={textoErrorServicio(t, ruta, errores[ruta])}
              />
            </li>
          )
        })}
      </ul>
      <ErrorGrupo ruta="unidades" error={errorGrupo} />
    </fieldset>
  )
}
