import { useEffect, useId, useRef, useState, type RefObject } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { iniciales } from './iniciales'

export interface UsuarioEncabezado {
  nombreVisible: string
  correo: string
  /** «Mis reservas» (WF-11) solo aparece para el rol Turista. */
  rol?: string
}

/**
 * Avatar con iniciales y menú: «Mis reservas» (solo turistas, WF-11) y «Cerrar sesión» (RF55). Accesible con
 * teclado: Enter abre y lleva el foco a la primera opción, Escape cierra y devuelve el foco al avatar.
 */
export function MenuUsuario({ usuario, onCerrarSesion }: { usuario: UsuarioEncabezado; onCerrarSesion: () => void }) {
  const { t } = useTranslation()
  const [abierto, setAbierto] = useState(false)
  const idMenu = useId()
  const contenedor = useRef<HTMLDivElement>(null)
  const boton = useRef<HTMLButtonElement>(null)
  const primeraOpcion = useRef<HTMLElement>(null)
  const esTurista = usuario.rol === 'Turista'

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
            {esTurista && (
              <li role="none">
                <Link
                  ref={primeraOpcion as RefObject<HTMLAnchorElement>}
                  to="/mis-reservas"
                  role="menuitem"
                  onClick={() => setAbierto(false)}
                >
                  {t('encabezado.misReservas')}
                </Link>
              </li>
            )}
            <li role="none">
              <button
                ref={esTurista ? undefined : (primeraOpcion as RefObject<HTMLButtonElement>)}
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
