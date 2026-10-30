import { useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { MAXIMO_UNIDADES, unidadVacia, type UnidadBorrador } from '../borradorServicio'
import { idDeCampo, textoErrorServicio, type ErroresServicio } from '../rutasCampos'
import { CampoServicio, ErrorGrupo } from './CampoServicio'

const RUTA = 'detalleCategoria.unidades'

/**
 * Unidades de un alojamiento (RF10, DEC-133 y DEC-164): de 1 a 20 filas. «Quitar» no está con una sola unidad y
 * «Agregar» se detiene en 20. Al quitar una fila, los índices cambian: `onQuitar` limpia los errores de las unidades y
 * el foco va a la fila anterior (o a «Agregar unidad»).
 */
export function EditorUnidades({
  unidades,
  errores,
  onCambiar,
  onQuitar,
}: {
  unidades: UnidadBorrador[]
  errores: ErroresServicio
  onCambiar: (unidades: UnidadBorrador[]) => void
  onQuitar: (unidades: UnidadBorrador[]) => void
}) {
  const { t } = useTranslation()
  const agregar = useRef<HTMLButtonElement>(null)
  const errorGrupo = textoErrorServicio(t, RUTA, errores[RUTA])
  const cambiar = (indice: number, campo: keyof Omit<UnidadBorrador, 'clave'>, valor: string) =>
    onCambiar(unidades.map((unidad, i) => (i === indice ? { ...unidad, [campo]: valor } : unidad)))
  const quitar = (indice: number) => {
    onQuitar(unidades.filter((_unidad, i) => i !== indice))
    const anterior = Math.max(0, indice - 1)
    // Después del render: la fila anterior conserva su id; si no hay, el botón de agregar.
    setTimeout(() => (document.getElementById(idDeCampo(`${RUTA}[${anterior}].nombre`)) ?? agregar.current)?.focus(), 0)
  }

  return (
    <fieldset
      id={idDeCampo(RUTA)}
      tabIndex={-1}
      className="prestador__grupo"
      aria-describedby={errorGrupo ? `${idDeCampo(RUTA)}-error` : undefined}
    >
      <legend>{t('prestador.registro.unidades')}</legend>
      <p className="campo__ayuda">{t('prestador.registro.unidadesAyuda', { maximo: MAXIMO_UNIDADES })}</p>
      <ErrorGrupo ruta={RUTA} error={errorGrupo} />
      <ol className="prestador__filas">
        {unidades.map((unidad, indice) => {
          const ruta = (campo: string) => `${RUTA}[${indice}].${campo}`
          const error = (campo: string) => textoErrorServicio(t, ruta(campo), errores[ruta(campo)])
          return (
            <li key={unidad.clave} className="prestador__fila">
              <fieldset className="prestador__unidad">
                <legend>{t('prestador.registro.unidad', { numero: indice + 1 })}</legend>
                <div className="prestador__columnas">
                  <CampoServicio ruta={ruta('nombre')} etiqueta={t('prestador.campos.nombreUnidad')} valor={unidad.nombre}
                    onCambiar={(v) => cambiar(indice, 'nombre', v)} error={error('nombre')} />
                  <CampoServicio ruta={ruta('tipoHabitacion')} etiqueta={t('prestador.campos.tipoHabitacion')}
                    valor={unidad.tipoHabitacion} onCambiar={(v) => cambiar(indice, 'tipoHabitacion', v)}
                    error={error('tipoHabitacion')} />
                  <CampoServicio ruta={ruta('numHabitaciones')} etiqueta={t('prestador.campos.numHabitaciones')}
                    inputMode="numeric" valor={unidad.numHabitaciones}
                    onCambiar={(v) => cambiar(indice, 'numHabitaciones', v)} error={error('numHabitaciones')} />
                  <CampoServicio ruta={ruta('capacidadMaxHuespedes')} etiqueta={t('prestador.campos.capacidadMaxHuespedes')}
                    inputMode="numeric" valor={unidad.capacidadMaxHuespedes}
                    onCambiar={(v) => cambiar(indice, 'capacidadMaxHuespedes', v)} error={error('capacidadMaxHuespedes')} />
                  <CampoServicio ruta={ruta('precioNoche')} etiqueta={t('prestador.campos.precioNoche')}
                    inputMode="decimal" sufijo="COP" valor={unidad.precioNoche}
                    onCambiar={(v) => cambiar(indice, 'precioNoche', v)} error={error('precioNoche')} />
                </div>
                <button
                  type="button"
                  className="boton-secundario prestador__quitar"
                  disabled={unidades.length === 1}
                  onClick={() => quitar(indice)}
                >
                  {t('prestador.registro.quitarUnidad', { numero: indice + 1 })}
                </button>
              </fieldset>
            </li>
          )
        })}
      </ol>
      <button
        ref={agregar}
        type="button"
        className="boton-secundario"
        disabled={unidades.length >= MAXIMO_UNIDADES}
        onClick={() => onCambiar([...unidades, unidadVacia()])}
      >
        {t('prestador.registro.agregarUnidad')}
      </button>
    </fieldset>
  )
}
