import { useTranslation } from 'react-i18next'

/** Números de página con elipsis cuando hay muchas: 1 … 4 5 6 … 12. */
export function paginasVisibles(actual: number, total: number): (number | '…')[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const paginas: (number | '…')[] = [1]
  const desde = Math.max(2, actual - 1)
  const hasta = Math.min(total - 1, actual + 1)
  if (desde > 2) paginas.push('…')
  for (let p = desde; p <= hasta; p++) paginas.push(p)
  if (hasta < total - 1) paginas.push('…')
  paginas.push(total)
  return paginas
}

/** Paginación numerada de WF-05 (RF16: 10 elementos por página). Las páginas empiezan en 1. */
export function Paginacion({
  actual,
  total,
  onCambiar,
}: {
  actual: number
  total: number
  onCambiar: (pagina: number) => void
}) {
  const { t } = useTranslation()
  if (total <= 1) return null

  return (
    <nav className="paginacion" aria-label={t('explorar.paginacion')}>
      <ul>
        <li>
          <button type="button" disabled={actual <= 1} onClick={() => onCambiar(actual - 1)}>
            <span aria-hidden="true">‹ </span>
            {t('explorar.anterior')}
          </button>
        </li>
        {paginasVisibles(actual, total).map((pagina, indice) =>
          pagina === '…' ? (
            <li key={`elipsis-${indice}`} className="paginacion__elipsis" aria-hidden="true">
              …
            </li>
          ) : (
            <li key={pagina}>
              <button
                type="button"
                aria-current={pagina === actual ? 'page' : undefined}
                aria-label={t('explorar.pagina', { numero: pagina })}
                onClick={() => onCambiar(pagina)}
              >
                {pagina}
              </button>
            </li>
          ),
        )}
        <li>
          <button type="button" disabled={actual >= total} onClick={() => onCambiar(actual + 1)}>
            {t('explorar.siguiente')}
            <span aria-hidden="true"> ›</span>
          </button>
        </li>
      </ul>
    </nav>
  )
}
