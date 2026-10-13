import { useId, useState, type FormEvent } from 'react'
import type { TFunction } from 'i18next'
import { useTranslation } from 'react-i18next'
import {
  CONDICIONES_ACCESIBILIDAD,
  soloDeCategoria,
  type Categoria,
  type CondicionAccesibilidad,
  type CriteriosBusqueda,
  type MasFiltros as Filtros,
} from '../catalogoApi'
import { formatearHoras, formatearPrecio } from '../formato'
import { etiquetaCategoria } from './etiquetas'
import { MasFiltros } from './MasFiltros'

export const PRECIO_MAXIMO_DESLIZADOR = 500_000
const PASO_PRECIO = 10_000
const DISTANCIA_MAXIMA_DESLIZADOR = 50

export type CambiosFiltros = Pick<
  CriteriosBusqueda,
  'texto' | 'categoria' | 'masFiltros' | 'precioMax' | 'distanciaKm' | 'accesibilidad'
>

/** Un chip por filtro de «Más filtros» aplicado: su texto y los filtros que quedan al quitarlo. */
interface ChipMasFiltro {
  clave: string
  texto: string
  sin: Filtros
}

/**
 * Filtros de WF-05. El estado local parte de la URL (quien lo usa le pone key = la consulta, para reiniciarlo con
 * Atrás); «Aplicar filtros» lo escribe en la URL. Las 3 condiciones de RF50 son una Mejora de diseño (WF-05 solo
 * muestra movilidad reducida). La v2 dibuja «Rango de precio», pero RF16 pide precio máximo: manda el SRS.
 * «Más filtros» (RF10) agrega la categoría y los filtros propios de ella; también se aplican con «Aplicar filtros».
 */
export function FiltrosOferta({
  criterios,
  hayUbicacion,
  onAplicar,
  onQuitarCategoria,
  onQuitarMasFiltro,
}: {
  criterios: CriteriosBusqueda
  hayUbicacion: boolean
  onAplicar: (cambios: CambiosFiltros) => void
  onQuitarCategoria: () => void
  onQuitarMasFiltro: (restantes: Filtros) => void
}) {
  const { t, i18n } = useTranslation()
  const id = useId()
  const [texto, setTexto] = useState(criterios.texto ?? '')
  const [precioMax, setPrecioMax] = useState<number | undefined>(criterios.precioMax)
  const [distanciaKm, setDistanciaKm] = useState<number | undefined>(criterios.distanciaKm)
  const [accesibilidad, setAccesibilidad] = useState<Set<CondicionAccesibilidad>>(new Set(criterios.accesibilidad))
  const [categoria, setCategoria] = useState<Categoria | undefined>(criterios.categoria)
  const [masFiltros, setMasFiltros] = useState<Filtros>(criterios.masFiltros ?? {})

  const aplicar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault()
    onAplicar({
      texto: texto.trim() || undefined,
      categoria,
      masFiltros: soloDeCategoria(masFiltros, categoria),
      precioMax,
      distanciaKm,
      accesibilidad: CONDICIONES_ACCESIBILIDAD.filter((c) => accesibilidad.has(c)),
    })
  }

  const alternar = (condicion: CondicionAccesibilidad) =>
    setAccesibilidad((actual) => {
      const nueva = new Set(actual)
      if (nueva.has(condicion)) nueva.delete(condicion)
      else nueva.add(condicion)
      return nueva
    })

  const valorPrecio = precioMax === undefined ? PRECIO_MAXIMO_DESLIZADOR : Math.min(precioMax, PRECIO_MAXIMO_DESLIZADOR)
  const textoPrecio = precioMax === undefined ? t('explorar.sinLimite') : formatearPrecio(precioMax, i18n.language)
  const chips = chipsMasFiltros(soloDeCategoria(criterios.masFiltros, criterios.categoria), t, i18n.language)

  return (
    <section className="filtros" aria-labelledby={`${id}-titulo`}>
      <h2 id={`${id}-titulo`} className="filtros__titulo">
        {t('explorar.filtros')}
      </h2>

      {criterios.categoria && (
        <ul className="filtros__activos" aria-label={t('explorar.filtrosActivos')}>
          <li>
            <button
              type="button"
              className="chip"
              aria-label={t('explorar.quitarCategoria', { categoria: etiquetaCategoria(t, criterios.categoria) })}
              onClick={onQuitarCategoria}
            >
              {t('explorar.categoria', { categoria: etiquetaCategoria(t, criterios.categoria) })}
              <span aria-hidden="true"> ×</span>
            </button>
          </li>
          {chips.map((chip) => (
            <li key={chip.clave}>
              <button
                type="button"
                className="chip"
                aria-label={t('explorar.masFiltros.quitar', { filtro: chip.texto })}
                onClick={() => onQuitarMasFiltro(chip.sin)}
              >
                {chip.texto}
                <span aria-hidden="true"> ×</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      <form className="filtros__formulario" onSubmit={aplicar}>
        <div className="filtro filtro--texto">
          <label htmlFor={`${id}-texto`}>{t('explorar.texto')}</label>
          <input
            id={`${id}-texto`}
            type="search"
            maxLength={100}
            placeholder={t('explorar.textoPlaceholder')}
            value={texto}
            onChange={(evento) => setTexto(evento.target.value)}
          />
        </div>

        <div className="filtro">
          <label htmlFor={`${id}-precio`}>{t('explorar.precioMax')}</label>
          <input
            id={`${id}-precio`}
            type="range"
            min={0}
            max={PRECIO_MAXIMO_DESLIZADOR}
            step={PASO_PRECIO}
            value={valorPrecio}
            aria-valuetext={textoPrecio}
            onChange={(evento) => {
              const valor = Number(evento.target.value)
              setPrecioMax(valor >= PRECIO_MAXIMO_DESLIZADOR ? undefined : valor)
            }}
          />
          <output htmlFor={`${id}-precio`} className="filtro__valor">
            {textoPrecio}
          </output>
        </div>

        <div className="filtro">
          <label htmlFor={`${id}-distancia`}>{t('explorar.distanciaMax')}</label>
          <input
            id={`${id}-distancia`}
            type="range"
            min={1}
            max={DISTANCIA_MAXIMA_DESLIZADOR}
            step={1}
            value={distanciaKm ?? DISTANCIA_MAXIMA_DESLIZADOR}
            disabled={!hayUbicacion}
            aria-describedby={hayUbicacion ? undefined : `${id}-sin-ubicacion`}
            onChange={(evento) => setDistanciaKm(Number(evento.target.value))}
          />
          <output htmlFor={`${id}-distancia`} className="filtro__valor">
            {distanciaKm === undefined ? t('explorar.sinLimite') : `${distanciaKm} km`}
          </output>
          {!hayUbicacion && (
            <p id={`${id}-sin-ubicacion`} className="filtro__ayuda">
              {t('explorar.sinUbicacion')}
            </p>
          )}
        </div>

        <fieldset className="filtro filtro--accesibilidad">
          <legend>{t('explorar.accesibilidad')}</legend>
          {CONDICIONES_ACCESIBILIDAD.map((condicion) => (
            <label key={condicion} className="casilla">
              <input type="checkbox" checked={accesibilidad.has(condicion)} onChange={() => alternar(condicion)} />
              {t(`catalogo.accesibilidad.${condicion}`)}
            </label>
          ))}
        </fieldset>

        <MasFiltros
          categoria={categoria}
          filtros={masFiltros}
          onCambiar={(nuevaCategoria, nuevos) => {
            setCategoria(nuevaCategoria)
            setMasFiltros(nuevos)
          }}
        />

        <button type="submit" className="boton-principal filtros__aplicar">
          {t('explorar.aplicar')}
        </button>
      </form>
    </section>
  )
}

/** Textos de los chips: los valores de los datos se muestran tal cual; los servicios incluidos, traducidos. */
function chipsMasFiltros(filtros: Filtros, t: TFunction, idioma: string): ChipMasFiltro[] {
  const chips: ChipMasFiltro[] = []
  const conValor = (etiqueta: string, valor: string) =>
    t('explorar.masFiltros.chip', { filtro: t(`explorar.masFiltros.${etiqueta}`), valor })
  filtros.serviciosIncluidos?.forEach((servicio) =>
    chips.push({
      clave: `serviciosIncluidos-${servicio}`,
      texto: t(`catalogo.serviciosIncluidos.${servicio}`),
      sin: { ...filtros, serviciosIncluidos: filtros.serviciosIncluidos?.filter((s) => s !== servicio) },
    }),
  )
  if (filtros.huespedes !== undefined) {
    chips.push({
      clave: 'huespedes',
      texto: t('explorar.masFiltros.chipHuespedes', { count: filtros.huespedes }),
      sin: { ...filtros, huespedes: undefined },
    })
  }
  for (const campo of ['tipoHabitacion', 'tipoCocina', 'nivelDificultad', 'tipoVehiculoEquipo'] as const) {
    const valor = filtros[campo]
    if (valor) chips.push({ clave: campo, texto: conValor(campo, valor), sin: { ...filtros, [campo]: undefined } })
  }
  if (filtros.menuVegetariano) {
    chips.push({
      clave: 'menuVegetariano',
      texto: t('explorar.masFiltros.opcionVegetariana'),
      sin: { ...filtros, menuVegetariano: undefined },
    })
  }
  if (filtros.duracionMaxHoras !== undefined) {
    chips.push({
      clave: 'duracionMaxHoras',
      texto: conValor('duracionMax', t('explorar.masFiltros.horas', { horas: formatearHoras(filtros.duracionMaxHoras, idioma) })),
      sin: { ...filtros, duracionMaxHoras: undefined },
    })
  }
  return chips
}
