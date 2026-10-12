import { useTranslation } from 'react-i18next'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useSesion } from '../features/auth/useSesion'

/** Sin sesión, cualquier ruta privada lleva al login y recuerda a dónde se quería ir. */
export function RutaProtegida() {
  const { t } = useTranslation()
  const { sesion } = useSesion()
  const ubicacion = useLocation()

  if (sesion.estado === 'cargando') {
    return (
      <p className="estado-carga" role="status" aria-busy="true">
        {t('comun.cargando')}
      </p>
    )
  }
  if (sesion.estado === 'anonimo') {
    // Ruta completa (con ?consulta y #ancla) para volver exactamente a donde se quería ir.
    return <Navigate to="/login" replace state={{ desde: ubicacion.pathname + ubicacion.search + ubicacion.hash }} />
  }
  return <Outlet />
}
