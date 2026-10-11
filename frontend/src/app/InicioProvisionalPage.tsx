import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { useSesion } from '../features/auth/useSesion'
import { MarcaSamaria, Ola } from '../shared/components/Ilustraciones'

/**
 * Destino provisional tras el login. La pantalla real es WF-04 (inicio y búsqueda) y llegará con el
 * catálogo; entonces este componente se reemplaza.
 */
export function InicioProvisionalPage() {
  const { t } = useTranslation()
  const { sesion } = useSesion()
  const titulo = useRef<HTMLHeadingElement>(null)

  // Al llegar desde el login, el foco va al título para que el lector de pantalla anuncie la nueva página.
  useEffect(() => {
    titulo.current?.focus()
  }, [])

  if (sesion.estado !== 'autenticado') {
    return null
  }
  return (
    <div className="inicio-provisional">
      <div className="inicio-provisional__decoracion" aria-hidden="true">
        <MarcaSamaria tamano={72} />
      </div>
      <h1 ref={titulo} tabIndex={-1}>
        {t('inicio.saludo', { nombre: sesion.usuario.nombreVisible })}
      </h1>
      <p>{t('inicio.provisional')}</p>
      <Ola className="inicio-provisional__ola" />
    </div>
  )
}
