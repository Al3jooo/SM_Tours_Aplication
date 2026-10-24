import { useTranslation } from 'react-i18next'
import { CATEGORIAS_INTERES } from './validacionRegistro'

/** Marca o desmarca una categoría y deja la lista en el orden del vocabulario (D1 del registro). */
export function alternarCategoria(actuales: readonly string[], categoria: string): string[] {
  return actuales.includes(categoria)
    ? actuales.filter((c) => c !== categoria)
    : CATEGORIAS_INTERES.filter((c) => c === categoria || actuales.includes(c))
}

/** Id de la casilla de una categoría: el primer campo con error de las categorías recibe el foco ahí. */
export const idCasillaCategoria = (prefijo: string, categoria: string) => `${prefijo}-categoria-${categoria}`

/**
 * Categorías de interés turístico (RF01, RF53): una casilla por cada una del vocabulario cerrado (Naturaleza, Cultura y
 * Aventura), con su ayuda y su error conectados al grupo. Compartido por WF-02 y WF-16 (DEC-98).
 */
export function CasillasCategorias({
  prefijo,
  leyenda,
  ayuda,
  seleccionadas,
  onCambiar,
  error,
}: {
  /** Prefijo de los ids: `${prefijo}-categoria-*`, `${prefijo}-categoriasInteres-ayuda` y `-error`. */
  prefijo: string
  leyenda: string
  ayuda: string
  seleccionadas: string[]
  onCambiar: (categorias: string[]) => void
  /** Texto del error del campo, ya traducido. */
  error?: string
}) {
  const { t } = useTranslation()
  const idAyuda = `${prefijo}-categoriasInteres-ayuda`
  const idError = `${prefijo}-categoriasInteres-error`
  return (
    <fieldset className="registro__grupo" aria-describedby={error ? `${idAyuda} ${idError}` : idAyuda}>
      <legend>{leyenda}</legend>
      <p id={idAyuda} className="campo__ayuda">
        {ayuda}
      </p>
      {CATEGORIAS_INTERES.map((categoria) => (
        <label key={categoria} className="casilla">
          <input
            id={idCasillaCategoria(prefijo, categoria)}
            type="checkbox"
            checked={seleccionadas.includes(categoria)}
            onChange={() => onCambiar(alternarCategoria(seleccionadas, categoria))}
          />
          {t(`registro.categorias.${categoria}`)}
        </label>
      ))}
      {error && (
        <p id={idError} className="campo__error">
          {error}
        </p>
      )}
    </fieldset>
  )
}
