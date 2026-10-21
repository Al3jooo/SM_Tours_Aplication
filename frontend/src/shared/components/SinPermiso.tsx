import { useTranslation } from 'react-i18next'

/**
 * Estado «Sin permiso» (DEC-51): una sesión válida sin el rol de la pantalla (RutaProtegida con `rol`) o un 403
 * ACCESO_DENEGADO de la API. No cierra la sesión.
 */
export function SinPermiso() {
  const { t } = useTranslation()
  return (
    <div className="sin-permiso">
      <h1>{t('comun.sinPermisoTitulo')}</h1>
      <p role="alert">{t('comun.sinPermiso')}</p>
    </div>
  )
}
