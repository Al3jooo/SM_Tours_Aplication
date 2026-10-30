import { useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { DIAS_SEMANA, MAXIMO_SALIDAS, salidaVacia, type DiaSemana, type SalidaBorrador } from '../borradorServicio'
import { idDeCampo, textoErrorServicio, type ErroresServicio } from '../rutasCampos'
import { ErrorGrupo } from './CampoServicio'

/**
 * Oferta inicial (DEC-130, DEC-136 y DEC-164): días de operación con casillas y, solo en Tour, de 1 a 12 salidas
 * `HH:mm`. Las salidas se ordenan al enviar; un duplicado se marca, no se borra.
 */
export function EditorOferta({
  conSalidas,
  dias,
  salidas,
  errores,
  onCambiarDias,
  onCambiarSalidas,
  onQuitarSalida,
}: {
  conSalidas: boolean
  dias: DiaSemana[]
  salidas: SalidaBorrador[]
  errores: ErroresServicio
  onCambiarDias: (dias: DiaSemana[]) => void
  onCambiarSalidas: (salidas: SalidaBorrador[]) => void
  onQuitarSalida: (salidas: SalidaBorrador[]) => void
}) {
  const { t } = useTranslation()
  const agregar = useRef<HTMLButtonElement>(null)
  const error = (ruta: string) => textoErrorServicio(t, ruta, errores[ruta])
  const describir = (ruta: string) => (error(ruta) ? `${idDeCampo(ruta)}-error` : undefined)
  const alternarDia = (dia: DiaSemana) =>
    onCambiarDias(dias.includes(dia) ? dias.filter((d) => d !== dia) : DIAS_SEMANA.filter((d) => d === dia || dias.includes(d)))
  const quitar = (indice: number) => {
    onQuitarSalida(salidas.filter((_salida, i) => i !== indice))
    const anterior = Math.max(0, indice - 1)
    setTimeout(() => (document.getElementById(idDeCampo(`oferta.salidas[${anterior}]`)) ?? agregar.current)?.focus(), 0)
  }

  return (
    <div className="prestador__oferta" id={idDeCampo('oferta')} tabIndex={-1} aria-describedby={describir('oferta')}>
      <ErrorGrupo ruta="oferta" error={error('oferta')} />
      <fieldset
        id={idDeCampo('oferta.diasOperacion')}
        tabIndex={-1}
        className="prestador__grupo"
        aria-describedby={describir('oferta.diasOperacion')}
      >
        <legend>{t('prestador.registro.diasOperacion')}</legend>
        <ul className="prestador__casillas">
          {DIAS_SEMANA.map((dia) => (
            <li key={dia}>
              <label>
                <input type="checkbox" checked={dias.includes(dia)} onChange={() => alternarDia(dia)} />
                {t(`comun.dias.${dia}`)}
              </label>
            </li>
          ))}
        </ul>
        <button type="button" className="boton-secundario" onClick={() => onCambiarDias([...DIAS_SEMANA])}>
          {t('prestador.registro.todosLosDias')}
        </button>
        <ErrorGrupo ruta="oferta.diasOperacion" error={error('oferta.diasOperacion')} />
      </fieldset>

      {conSalidas && (
        <fieldset
          id={idDeCampo('oferta.salidas')}
          tabIndex={-1}
          className="prestador__grupo"
          aria-describedby={describir('oferta.salidas')}
        >
          <legend>{t('prestador.registro.salidas')}</legend>
          <p className="campo__ayuda">{t('prestador.registro.salidasAyuda', { maximo: MAXIMO_SALIDAS })}</p>
          <ErrorGrupo ruta="oferta.salidas" error={error('oferta.salidas')} />
          <ol className="prestador__filas">
            {salidas.map((salida, indice) => {
              const ruta = `oferta.salidas[${indice}]`
              const id = idDeCampo(ruta)
              return (
                <li key={salida.clave} className="prestador__fila prestador__fila--salida">
                  <div className="campo">
                    <label htmlFor={id}>{t('prestador.registro.salida', { numero: indice + 1 })}</label>
                    <div className="campo__control">
                      <input
                        id={id}
                        type="time"
                        value={salida.hora}
                        aria-invalid={error(ruta) ? true : undefined}
                        aria-describedby={describir(ruta)}
                        onChange={(evento) =>
                          onCambiarSalidas(salidas.map((s, i) => (i === indice ? { ...s, hora: evento.target.value } : s)))
                        }
                      />
                    </div>
                    <ErrorGrupo ruta={ruta} error={error(ruta)} />
                  </div>
                  <button
                    type="button"
                    className="boton-secundario prestador__quitar"
                    disabled={salidas.length === 1}
                    onClick={() => quitar(indice)}
                  >
                    {t('prestador.registro.quitarSalida', { numero: indice + 1 })}
                  </button>
                </li>
              )
            })}
          </ol>
          <button
            ref={agregar}
            type="button"
            className="boton-secundario"
            disabled={salidas.length >= MAXIMO_SALIDAS}
            onClick={() => onCambiarSalidas([...salidas, salidaVacia()])}
          >
            {t('prestador.registro.agregarSalida')}
          </button>
        </fieldset>
      )}
    </div>
  )
}
