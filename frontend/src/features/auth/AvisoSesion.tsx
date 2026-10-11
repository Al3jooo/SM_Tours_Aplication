import { useTranslation } from 'react-i18next'
import { Link, useLocation } from 'react-router-dom'
import { useSesion } from './useSesion'

/**
 * Aviso no intrusivo cuando una sesión guardada deja de ser válida mientras se navega por una página pública:
 * el usuario se queda donde está (Mejora de diseño, docs/trazabilidad.md). No se muestra en el propio login.
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
        {t(aviso === 'expirada' ? 'sesion.avisoExpirada' : 'sesion.avisoInactiva')}{' '}
        <Link to="/login" onClick={descartarAviso}>
          {t(aviso === 'expirada' ? 'sesion.iniciarDeNuevo' : 'sesion.iniciarSesion')}
        </Link>
      </p>
      <button type="button" className="aviso-sesion__cerrar" aria-label={t('comun.cerrarAviso')} onClick={descartarAviso}>
        <span aria-hidden="true">×</span>
      </button>
    </div>
  )
}
