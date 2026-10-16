import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { horaCorta } from '../fechasBogota'
import type { CupoHorario } from '../reservasApi'

/**
 * Horarios publicados de un día de tour (WF-10): un radio por hora con su cupo. Un horario agotado queda deshabilitado
 * y lo dice en texto («Agotado»), no solo con el color.
 */
export function HorariosTour({
  horarios,
  seleccionado,
  onElegir,
}: {
  horarios: CupoHorario[]
  seleccionado: string | undefined
  onElegir: (hora: string) => void
}) {
  const { t } = useTranslation()
  const id = useId()

  return (
    <fieldset className="reserva__grupo reserva__horarios">
      <legend>{t('reserva.horario')}</legend>
      <ul>
        {horarios.map((horario) => {
          const hora = horaCorta(horario.hora)
          const agotado = horario.cuposDisponibles <= 0
          const idRadio = `${id}-${hora}`
          return (
            <li key={horario.hora} className={`opcion-horario${agotado ? ' opcion-horario--agotado' : ''}`}>
              <input
                type="radio"
                id={idRadio}
                name={`${id}-hora`}
                value={hora}
                checked={seleccionado === hora}
                disabled={agotado}
                onChange={() => onElegir(hora)}
              />
              <label htmlFor={idRadio}>
                <span className="opcion-horario__hora">{hora}</span>
                {' · '}
                <span className="opcion-horario__cupo">
                  {agotado ? t('reserva.agotado') : t('reserva.quedanCupos', { count: horario.cuposDisponibles })}
                </span>
              </label>
            </li>
          )
        })}
      </ul>
    </fieldset>
  )
}
