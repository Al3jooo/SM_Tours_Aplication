import { useTranslation } from 'react-i18next'
import { Link, useLocation } from 'react-router-dom'
import { TEXTO_AVISO } from './SesionContext'
import { useSesion } from './useSesion'

/**
 * Aviso no intrusivo cuando una sesión guardada deja de ser válida mientras se navega por una página pública:
 * el usuario se queda donde está (Mejora de diseño, docs/trazabilidad.md). En el login lo muestra LoginPage, sin enlace
 * (DEC-50).
 */
export function AvisoSesion() {
  const { t } = useTranslation()
  const { aviso, descartarAviso } = useSesion()
  const { pathname } = useLocation()

  if (!aviso || pathname === '/login') {
    return null
  }
  return (
    <div className="aviso-sesion" role="status">
      <p>
        {t(TEXTO_AVISO[aviso].aviso)}{' '}
        <Link to="/login" onClick={descartarAviso}>
          {t(TEXTO_AVISO[aviso].enlace)}
        </Link>
      </p>
      <button type="button" className="aviso-sesion__cerrar" aria-label={t('comun.cerrarAviso')} onClick={descartarAviso}>
        <span aria-hidden="true">×</span>
      </button>
    </div>
  )
}
