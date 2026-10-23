import { useEffect, useId } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation } from 'react-router-dom'
import { terminosDe } from './textoTerminos'
import '../contenido.css'

/**
 * WF-15, términos y condiciones (CU «Consultar términos y condiciones», RF46): página estática y pública, en el idioma
 * activo (RNF08), con la nota de prototipo académico y el índice de DEC-68. Si llega con un ancla (p. ej.
 * `#privacidad` desde «Políticas de privacidad»), lleva a esa sección y le da el foco (DEC-75). Cambiar de idioma
 * conserva la estructura, así que la posición no se pierde.
 */
export function TerminosPage() {
  const { t, i18n } = useTranslation()
  const id = useId()
  const { hash } = useLocation()
  const secciones = terminosDe(i18n.language)

  useEffect(() => {
    if (!hash) return
    const seccion = document.getElementById(decodeURIComponent(hash.slice(1)))
    if (!seccion) return
    // jsdom no implementa scrollIntoView.
    seccion.scrollIntoView?.({ block: 'start' })
    seccion.querySelector<HTMLElement>('h2')?.focus()
  }, [hash])

  return (
    <article className="terminos" aria-labelledby={`${id}-titulo`}>
      <h1 id={`${id}-titulo`}>{t('terminos.titulo')}</h1>
      <p className="terminos__nota">{t('terminos.nota')}</p>

      <nav className="terminos__indice" aria-labelledby={`${id}-indice`}>
        <h2 id={`${id}-indice`}>{t('terminos.indice')}</h2>
        <ol>
          {secciones.map((seccion) => (
            <li key={seccion.id}>
              <a href={`#${seccion.id}`}>{seccion.titulo.replace(/^\d+\.\s*/, '')}</a>
            </li>
          ))}
        </ol>
      </nav>

      {secciones.map((seccion) => (
        <section key={seccion.id} id={seccion.id} className="terminos__seccion" aria-labelledby={`${id}-${seccion.id}`}>
          <h2 id={`${id}-${seccion.id}`} tabIndex={-1}>
            {seccion.titulo}
          </h2>
          {seccion.parrafos.map((parrafo) => (
            <p key={parrafo}>{parrafo}</p>
          ))}
        </section>
      ))}
    </article>
  )
}
