import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'

/**
 * Encabezado común de WF-01, WF-02 y WF-03: subtítulo y pestañas «Iniciar sesión» / «Registrarse». Las pestañas cambian
 * de página (/login y /registro), así que son enlaces con `aria-current`, no un `tablist`.
 */
export function PestanasAcceso({ activa }: { activa: 'login' | 'registro' }) {
  const { t } = useTranslation()
  return (
    <div className="login__encabezado">
      <p className="login__subtitulo">{t('login.subtitulo')}</p>
      <nav className="pestanas" aria-label={t('login.pestanas')}>
        <Link to="/login" aria-current={activa === 'login' ? 'page' : undefined}>
          {t('login.pestanaIniciar')}
        </Link>
        <Link to="/registro" aria-current={activa === 'registro' ? 'page' : undefined}>
          {t('login.pestanaRegistrarse')}
        </Link>
      </nav>
    </div>
  )
}
