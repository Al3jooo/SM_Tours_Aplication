import { useTranslation } from 'react-i18next'
import { Ola } from './Ilustraciones'
import { LogoSamaria } from './LogoSamaria'

const COLUMNAS = [
  { titulo: 'pie.explorar', enlaces: ['pie.alojamiento', 'pie.gastronomia', 'pie.tours', 'pie.alquilerEquipos'] },
  { titulo: 'pie.municipio', enlaces: ['pie.noticias', 'pie.emergencias', 'pie.afluencia', 'pie.contacto'] },
  { titulo: 'pie.prestadores', enlaces: ['pie.registrarServicio', 'pie.panelGestor', 'pie.preguntasFrecuentes'] },
  { titulo: 'pie.legal', enlaces: ['pie.terminos'] },
] as const

/**
 * Pie común según docs/wireframes/README.md. Ningún destino existe todavía en esta iteración, así que
 * los enlaces se muestran como texto deshabilitado (pendientes en docs/trazabilidad.md).
 */
export function PiePagina() {
  const { t } = useTranslation()
  return (
    <footer className="pie">
      <Ola className="pie__ola" />
      <div className="pie__contenido">
        <p className="logo logo--pie">
          {/* Sobre mar-900 la isla marrón y las olas se pierden: el ícono va en un círculo claro. */}
          <span className="logo__insignia">
            <LogoSamaria tamano={36} />
          </span>
          Samaria360
        </p>
        {COLUMNAS.map((columna) => (
          <section key={columna.titulo} className="pie__columna" aria-labelledby={`pie-${columna.titulo}`}>
            <h2 id={`pie-${columna.titulo}`}>{t(columna.titulo)}</h2>
            <ul>
              {columna.enlaces.map((enlace) => (
                <li key={enlace}>
                  <span className="enlace-deshabilitado" title={t('comun.proximamente')}>
                    {t(enlace)}
                  </span>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>
      <div className="pie__derechos">
        <p>{t('pie.derechos')}</p>
        {/* Las fotos del catálogo no tienen créditos verificados (CLAUDE.md, «Imágenes del catálogo»). */}
        <p className="pie__aviso">{t('pie.avisoAcademico')}</p>
      </div>
    </footer>
  )
}
