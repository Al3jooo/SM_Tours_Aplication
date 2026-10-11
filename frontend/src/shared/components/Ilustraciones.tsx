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

/** Escena del login: atardecer sobre el mar, con la silueta nevada de la Sierra Nevada. */
export function EscenaSantaMarta() {
  const id = useId()
  return (
    <svg
      className="escena-santa-marta"
      viewBox="0 0 480 600"
      preserveAspectRatio="xMidYMid slice"
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <linearGradient id={`${id}-cielo`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" style={parada('sol-100')} />
          <stop offset="0.38" style={parada('sol-400')} />
          <stop offset="0.62" style={parada('atardecer-500')} />
          <stop offset="0.74" style={parada('atardecer-600')} />
        </linearGradient>
        <linearGradient id={`${id}-mar`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" style={parada('mar-700')} />
          <stop offset="1" style={parada('mar-900')} />
        </linearGradient>
        <radialGradient id={`${id}-brillo`}>
          <stop offset="0" style={parada('sol-100', 0.9)} />
          <stop offset="1" style={parada('sol-100', 0)} />
        </radialGradient>
      </defs>

      {/* Cielo y sol sobre el horizonte */}
      <rect width="480" height="600" style={{ fill: `url(#${id}-cielo)` }} />
      <circle cx="330" cy="415" r="120" style={{ fill: `url(#${id}-brillo)` }} />
      <circle cx="330" cy="420" r="62" style={color('sol-100')} />

      {/* Aves */}
      <g style={{ fill: 'none', stroke: 'var(--color-mar-900)', strokeWidth: 2.5, strokeLinecap: 'round', opacity: 0.55 }}>
        <path d="M96 150q9-8 16 0 7-8 16 0" />
        <path d="M140 118q6-5 11 0 5-5 11 0" />
        <path d="M70 196q5-4 9 0 4-4 9 0" />
      </g>

      {/* Sierra Nevada: cordillera lejana con picos nevados y cordillera cercana */}
      <path
        d="M0 332 55 282l45 25 60-82 45 45 45-25 50 55 40 32v100H0z"
        style={{ ...color('mar-900'), opacity: 0.55 }}
      />
      <path d="m141 251 19-26 19 26-9-5-10 9-10-9z" style={color('superficie')} />
      <path d="m233 260 17-15 18 17-9-3-9 7-8-6z" style={color('superficie')} />
      <path d="M0 382 45 347l45 23 50-38 50 42 45-22 45 50 20 30H0z" style={color('sierra-700')} />

      {/* Mar Caribe con el reflejo del sol */}
      <rect y="430" width="480" height="170" style={{ fill: `url(#${id}-mar)` }} />
      <g style={{ ...color('sol-100'), opacity: 0.75 }}>
        <rect x="282" y="444" width="96" height="5" rx="2.5" />
        <rect x="296" y="460" width="68" height="4" rx="2" />
        <rect x="308" y="475" width="44" height="4" rx="2" />
        <rect x="318" y="490" width="24" height="3" rx="1.5" />
      </g>
      <path
        d="M0 500q30-12 60 0t60 0 60 0 60 0 60 0 60 0 60 0 60 0V600H0z"
        style={{ ...color('mar-500'), opacity: 0.5 }}
      />
      <path d="M0 545q30-12 60 0t60 0 60 0 60 0 60 0 60 0 60 0 60 0V600H0z" style={{ ...color('mar-900'), opacity: 0.7 }} />
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
