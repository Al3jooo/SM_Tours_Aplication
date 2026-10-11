import type { SVGProps } from 'react'

// Iconos decorativos en línea (sin librerías): siempre aria-hidden; el texto accesible lo da el control.
function Icono(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      {...props}
    />
  )
}

export function IconoSobre() {
  return (
    <Icono>
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="m3 7 9 6 9-6" />
    </Icono>
  )
}

export function IconoCandado() {
  return (
    <Icono>
      <rect x="5" y="11" width="14" height="10" rx="2" />
      <path d="M8 11V7a4 4 0 0 1 8 0v4" />
    </Icono>
  )
}

export function IconoRayo() {
  return (
    <Icono>
      <path d="M13 2 4 14h7l-1 8 9-12h-7l1-8z" />
    </Icono>
  )
}

export function IconoEntrar() {
  return (
    <Icono>
      <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
      <path d="m10 17 5-5-5-5" />
      <path d="M15 12H3" />
    </Icono>
  )
}

export function IconoBuscar() {
  return (
    <Icono>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </Icono>
  )
}

export function IconoAsistente() {
  return (
    <Icono>
      <rect x="4" y="8" width="16" height="12" rx="3" />
      <path d="M12 8V4" />
      <circle cx="12" cy="3" r="1" />
      <path d="M9 14h.01M15 14h.01" />
    </Icono>
  )
}
