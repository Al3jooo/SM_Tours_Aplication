import { useTranslation } from 'react-i18next'
import { Outlet, useNavigate } from 'react-router-dom'
import { useSesion } from '../features/auth/useSesion'
import { BotonAsistente } from '../shared/components/BotonAsistente'
import { Encabezado } from '../shared/components/Encabezado'
import { PiePagina } from '../shared/components/PiePagina'

/** Elementos comunes a todas las pantallas (docs/wireframes/README.md). */
export function DisenoPrincipal() {
  const { t } = useTranslation()
  const { sesion, cerrarSesion } = useSesion()
  const navegar = useNavigate()

  return (
    <div className="pagina">
      <a className="saltar-contenido" href="#contenido">
        {t('comun.saltarAlContenido')}
      </a>
      <Encabezado
        usuario={sesion.estado === 'autenticado' ? sesion.usuario : null}
        onCerrarSesion={() => {
          cerrarSesion()
          navegar('/login', { replace: true })
        }}
      />
      <main id="contenido" className="contenido" tabIndex={-1}>
        <Outlet />
      </main>
      <PiePagina />
      <BotonAsistente />
    </div>
  )
}
