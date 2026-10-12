import { useId, type CSSProperties } from 'react'

// Ilustraciones propias de la identidad Santa Marta (mar Caribe, Sierra Nevada, atardecer).
// Son decorativas: siempre aria-hidden y fuera del orden de tabulación. Los colores salen de los
// tokens de estilos.css (var(--color-...)), así que un cambio de paleta las actualiza.

const color = (token: string): CSSProperties => ({ fill: `var(--color-${token})` })
const parada = (token: string, opacidad = 1): CSSProperties => ({
  stopColor: `var(--color-${token})`,
  stopOpacity: opacidad,
})

/** Marca del logo: sol poniéndose sobre dos olas del Caribe. */
export function MarcaSamaria({ tamano = 32 }: { tamano?: number }) {
  const id = useId()
  return (
    <svg
      className="marca-samaria"
      width={tamano}
      height={tamano}
      viewBox="0 0 32 32"
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <linearGradient id={`${id}-cielo`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" style={parada('sol-400')} />
          <stop offset="0.6" style={parada('atardecer-500')} />
          <stop offset="1" style={parada('atardecer-600')} />
        </linearGradient>
        <clipPath id={`${id}-recorte`}>
          <rect width="32" height="32" rx="9" />
        </clipPath>
      </defs>
      <g clipPath={`url(#${id}-recorte)`}>
        <rect width="32" height="32" style={{ fill: `url(#${id}-cielo)` }} />
        <circle cx="16" cy="18" r="6.5" style={color('sol-100')} />
        <path d="M0 20.5q4-2.6 8 0t8 0 8 0 8 0V32H0z" style={color('mar-700')} />
        <path d="M0 25.5q4-2.6 8 0t8 0 8 0 8 0V32H0z" style={color('mar-900')} />
      </g>
    </svg>
  )
}

/** Ola horizontal; toma el color de `currentColor`. */
export function Ola({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 1440 48"
      preserveAspectRatio="none"
      aria-hidden="true"
      focusable="false"
    >
      <path
        d="M0 24C120 6 240 6 360 24s240 18 360 0 240-18 360 0 240 18 360 0V48H0z"
        fill="currentColor"
      />
    </svg>
  )
}
