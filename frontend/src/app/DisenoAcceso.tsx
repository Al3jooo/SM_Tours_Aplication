import { useTranslation } from 'react-i18next'
import { Outlet } from 'react-router-dom'
import { AvisoSesion } from '../features/auth/AvisoSesion'
import { EncabezadoAcceso } from '../shared/components/Encabezado'
import { PieAcceso } from '../shared/components/PiePagina'

/**
 * Pantallas de acceso (WF-01 a WF-03, docs/wireframes/README.md v2): encabezado y pie reducidos, sin navegación.
 */
export function DisenoAcceso() {
  const { t } = useTranslation()

  return (
    <div className="pagina">
      <a className="saltar-contenido" href="#contenido">
        {t('comun.saltarAlContenido')}
      </a>
      <EncabezadoAcceso />
      <AvisoSesion />
      <main id="contenido" className="contenido" tabIndex={-1}>
        <Outlet />
      </main>
      <PieAcceso />
    </div>
  )
}
