import { useId, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { CONDICIONES_ACCESIBILIDAD, type CondicionAccesibilidad, type CriteriosBusqueda } from '../catalogoApi'
import { formatearPrecio } from '../formato'
import { etiquetaCategoria } from './etiquetas'

export const PRECIO_MAXIMO_DESLIZADOR = 500_000
const PASO_PRECIO = 10_000
const DISTANCIA_MAXIMA_DESLIZADOR = 50

export type CambiosFiltros = Pick<CriteriosBusqueda, 'texto' | 'precioMax' | 'distanciaKm' | 'accesibilidad'>

/**
 * Filtros de WF-05. El estado local parte de la URL (quien lo usa le pone key = la consulta, para reiniciarlo con
 * Atrás); «Aplicar filtros» lo escribe en la URL. Las 3 condiciones de RF50 son una Mejora de diseño (WF-05 solo
 * muestra movilidad reducida).
 */
export function FiltrosOferta({
  criterios,
  hayUbicacion,
  onAplicar,
  onQuitarCategoria,
}: {
  criterios: CriteriosBusqueda
  hayUbicacion: boolean
  onAplicar: (cambios: CambiosFiltros) => void
  onQuitarCategoria: () => void
}) {
  const { t, i18n } = useTranslation()
  const id = useId()
  const [texto, setTexto] = useState(criterios.texto ?? '')
  const [precioMax, setPrecioMax] = useState<number | undefined>(criterios.precioMax)
  const [distanciaKm, setDistanciaKm] = useState<number | undefined>(criterios.distanciaKm)
  const [accesibilidad, setAccesibilidad] = useState<Set<CondicionAccesibilidad>>(new Set(criterios.accesibilidad))

  const aplicar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault()
    onAplicar({
      texto: texto.trim() || undefined,
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

  return (
    <section className="filtros" aria-labelledby={`${id}-titulo`}>
      <h2 id={`${id}-titulo`} className="filtros__titulo">
        {t('explorar.filtros')}
      </h2>

      {criterios.categoria && (
        <p className="filtros__activos">
          <button
            type="button"
            className="chip"
            aria-label={t('explorar.quitarCategoria', { categoria: etiquetaCategoria(t, criterios.categoria) })}
            onClick={onQuitarCategoria}
          >
            {t('explorar.categoria', { categoria: etiquetaCategoria(t, criterios.categoria) })}
            <span aria-hidden="true"> ×</span>
          </button>
        </p>
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

        <button type="submit" className="boton-principal filtros__aplicar">
          {t('explorar.aplicar')}
        </button>
      </form>
    </section>
  )
}
