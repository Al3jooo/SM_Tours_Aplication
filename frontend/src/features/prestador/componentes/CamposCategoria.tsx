import { useTranslation } from 'react-i18next'
import type { BorradorServicio } from '../borradorServicio'
import { idDeCampo, textoErrorServicio, type ErroresServicio } from '../rutasCampos'
import { CampoServicio, ErrorGrupo } from './CampoServicio'

type Seccion = 'tour' | 'gastronomia' | 'alquiler'

/**
 * Campos de la categoría (RF10) de Tour, Gastronomía y Alquiler de equipos, con las rutas de `detalleCategoria` del
 * contrato. Los de Alojamiento son las unidades (`EditorUnidades`).
 */
export function CamposCategoria({
  borrador,
  errores,
  onCambiar,
}: {
  borrador: BorradorServicio
  errores: ErroresServicio
  onCambiar: <S extends Seccion>(seccion: S, cambios: Partial<BorradorServicio[S]>) => void
}) {
  const { t } = useTranslation()
  const error = (campo: string) => textoErrorServicio(t, `detalleCategoria.${campo}`, errores[`detalleCategoria.${campo}`])
  const ruta = (campo: string) => `detalleCategoria.${campo}`

  if (borrador.categoria === 'Tour') {
    const tour = borrador.tour
    return (
      <div className="prestador__columnas">
        <CampoServicio ruta={ruta('puntoEncuentro')} etiqueta={t('prestador.campos.puntoEncuentro')}
          valor={tour.puntoEncuentro} onCambiar={(v) => onCambiar('tour', { puntoEncuentro: v })}
          error={error('puntoEncuentro')} />
        <CampoServicio ruta={ruta('duracionHoras')} etiqueta={t('prestador.campos.duracionHoras')} inputMode="decimal"
          valor={tour.duracionHoras} onCambiar={(v) => onCambiar('tour', { duracionHoras: v })}
          error={error('duracionHoras')} />
        <CampoServicio ruta={ruta('nivelDificultad')} etiqueta={t('prestador.campos.nivelDificultad')} opcional
          valor={tour.nivelDificultad} onCambiar={(v) => onCambiar('tour', { nivelDificultad: v })}
          error={error('nivelDificultad')} />
        <CampoServicio ruta={ruta('aforoMaxGrupo')} etiqueta={t('prestador.campos.aforoMaxGrupo')} inputMode="numeric"
          valor={tour.aforoMaxGrupo} onCambiar={(v) => onCambiar('tour', { aforoMaxGrupo: v })}
          error={error('aforoMaxGrupo')} />
      </div>
    )
  }

  if (borrador.categoria === 'Gastronomia') {
    const gastronomia = borrador.gastronomia
    const idVegetariano = idDeCampo(ruta('menuVegetariano'))
    return (
      <div className="prestador__columnas">
        <CampoServicio ruta={ruta('tipoCocina')} etiqueta={t('prestador.campos.tipoCocina')}
          valor={gastronomia.tipoCocina} onCambiar={(v) => onCambiar('gastronomia', { tipoCocina: v })}
          error={error('tipoCocina')} />
        <CampoServicio ruta={ruta('aforoMaxTurno')} etiqueta={t('prestador.campos.aforoMaxTurno')} inputMode="numeric"
          valor={gastronomia.aforoMaxTurno} onCambiar={(v) => onCambiar('gastronomia', { aforoMaxTurno: v })}
          error={error('aforoMaxTurno')} />
        <CampoServicio ruta={ruta('rangoPrecioPlato')} etiqueta={t('prestador.campos.rangoPrecioPlato')}
          valor={gastronomia.rangoPrecioPlato} onCambiar={(v) => onCambiar('gastronomia', { rangoPrecioPlato: v })}
          error={error('rangoPrecioPlato')} />
        <div className="campo prestador__casilla">
          <label htmlFor={idVegetariano}>
            <input
              id={idVegetariano}
              type="checkbox"
              checked={gastronomia.menuVegetariano}
              aria-describedby={error('menuVegetariano') ? `${idVegetariano}-error` : undefined}
              onChange={(evento) => onCambiar('gastronomia', { menuVegetariano: evento.target.checked })}
            />
            {t('prestador.campos.menuVegetariano')}
          </label>
          <ErrorGrupo ruta={ruta('menuVegetariano')} error={error('menuVegetariano')} />
        </div>
      </div>
    )
  }

  if (borrador.categoria === 'AlquilerEquipo') {
    const alquiler = borrador.alquiler
    return (
      <div className="prestador__columnas">
        <CampoServicio ruta={ruta('tipoVehiculoEquipo')} etiqueta={t('prestador.campos.tipoVehiculoEquipo')}
          valor={alquiler.tipoVehiculoEquipo} onCambiar={(v) => onCambiar('alquiler', { tipoVehiculoEquipo: v })}
          error={error('tipoVehiculoEquipo')} />
        <CampoServicio ruta={ruta('unidadesDisponibles')} etiqueta={t('prestador.campos.unidadesDisponibles')}
          inputMode="numeric" valor={alquiler.unidadesDisponibles}
          onCambiar={(v) => onCambiar('alquiler', { unidadesDisponibles: v })} error={error('unidadesDisponibles')} />
        <CampoServicio ruta={ruta('condicionesAlquiler')} etiqueta={t('prestador.campos.condicionesAlquiler')} opcional
          multilinea valor={alquiler.condicionesAlquiler}
          onCambiar={(v) => onCambiar('alquiler', { condicionesAlquiler: v })} error={error('condicionesAlquiler')} />
      </div>
    )
  }
  return null
}
