import { useTranslation } from 'react-i18next'
import { Link, NavLink } from 'react-router-dom'
import { IconoBuscar } from './Iconos'
import { LogoSamaria } from './LogoSamaria'
import { MenuUsuario, type UsuarioEncabezado } from './MenuUsuario'
import { BotonProximamente } from './Proximamente'
import { SelectorIdioma } from './SelectorIdioma'

/**
 * Encabezado común (WF-01, WF-04). Búsqueda lleva a la exploración (WF-05); Servicios y Asistente IA llegan en
 * iteraciones siguientes.
 * Recibe la sesión por props para que shared/ no dependa de features/.
 */
export function Encabezado({
  usuario,
  onCerrarSesion,
}: {
  usuario: UsuarioEncabezado | null
  onCerrarSesion: () => void
}) {
  const { t } = useTranslation()

  return (
    <header className="encabezado">
      <div className="encabezado__contenido">
        <Link to="/" className="logo" aria-label={t('encabezado.inicio')}>
          <LogoSamaria />
          <span aria-hidden="true">Samaria360</span>
        </Link>

        <nav className="navegacion" aria-label={t('encabezado.navegacion')}>
          <ul>
            <li>
              <NavLink to="/login">{t('encabezado.inicioSesion')}</NavLink>
            </li>
            <li>
              <NavLink to="/explorar">{t('encabezado.busqueda')}</NavLink>
            </li>
            <li>
              <BotonProximamente etiquetaVisible={false}>{t('encabezado.servicios')}</BotonProximamente>
            </li>
            <li>
              <BotonProximamente etiquetaVisible={false}>{t('encabezado.asistente')}</BotonProximamente>
            </li>
          </ul>
        </nav>

        <div className="encabezado__acciones">
          <SelectorIdioma />
          {usuario ? (
            // WF-04: con sesión iniciada el avatar ocupa el lugar del icono de búsqueda.
            <MenuUsuario usuario={usuario} onCerrarSesion={onCerrarSesion} />
          ) : (
            <BotonProximamente className="boton-icono" etiquetaAccesible={t('encabezado.buscar')}>
              <IconoBuscar />
            </BotonProximamente>
          )}
        </div>
      </div>
    </header>
  )
}
