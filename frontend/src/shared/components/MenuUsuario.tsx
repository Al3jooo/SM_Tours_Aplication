import { useEffect, useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { iniciales } from './iniciales'

export interface UsuarioEncabezado {
  nombreVisible: string
  correo: string
}

/** Avatar con iniciales y menú con Cerrar sesión (RF55). Accesible con teclado: Enter abre, Escape cierra. */
export function MenuUsuario({ usuario, onCerrarSesion }: { usuario: UsuarioEncabezado; onCerrarSesion: () => void }) {
  const { t } = useTranslation()
  const [abierto, setAbierto] = useState(false)
  const idMenu = useId()
  const contenedor = useRef<HTMLDivElement>(null)
  const boton = useRef<HTMLButtonElement>(null)
  const primeraOpcion = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!abierto) return
    primeraOpcion.current?.focus()
    const alPulsarFuera = (evento: MouseEvent) => {
      if (!contenedor.current?.contains(evento.target as Node)) setAbierto(false)
    }
    document.addEventListener('mousedown', alPulsarFuera)
    return () => document.removeEventListener('mousedown', alPulsarFuera)
  }, [abierto])

  const cerrarMenu = () => {
    setAbierto(false)
    boton.current?.focus()
  }

  return (
    <div
      className="menu-usuario"
      ref={contenedor}
      onKeyDown={(evento) => {
        if (evento.key === 'Escape' && abierto) {
          evento.stopPropagation()
          cerrarMenu()
        }
      }}
      onBlur={(evento) => {
        if (abierto && !contenedor.current?.contains(evento.relatedTarget as Node | null)) setAbierto(false)
      }}
    >
      <button
        ref={boton}
        type="button"
        className="avatar"
        aria-haspopup="menu"
        aria-expanded={abierto}
        aria-controls={abierto ? idMenu : undefined}
        aria-label={t('encabezado.menuUsuario', { nombre: usuario.nombreVisible })}
        onClick={() => setAbierto((valor) => !valor)}
      >
        <span aria-hidden="true">{iniciales(usuario.nombreVisible)}</span>
      </button>
      {abierto && (
        <div className="menu-usuario__panel">
          <p className="menu-usuario__nombre">{usuario.nombreVisible}</p>
          <p className="menu-usuario__correo">{usuario.correo}</p>
          <ul id={idMenu} role="menu" aria-label={usuario.nombreVisible}>
            <li role="none">
              <button
                ref={primeraOpcion}
                type="button"
                role="menuitem"
                onClick={() => {
                  setAbierto(false)
                  onCerrarSesion()
                }}
              >
                {t('encabezado.cerrarSesion')}
              </button>
            </li>
          </ul>
        </div>
      )}
    </div>
  )
}
