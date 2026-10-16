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

// --- Categorías del catálogo (WF-04) ---

export function IconoAlojamiento() {
  return (
    <Icono>
      <path d="M3 18V8" />
      <path d="M3 14h18v4" />
      <path d="M21 14v-2a3 3 0 0 0-3-3h-7v5" />
      <circle cx="7" cy="11" r="2" />
    </Icono>
  )
}

export function IconoGastronomia() {
  return (
    <Icono>
      <path d="M6 3v8a2 2 0 0 0 2 2v8" />
      <path d="M10 3v8a2 2 0 0 1-2 2" />
      <path d="M8 3v6" />
      <path d="M17 21V3c-2 1.5-3 4-3 7h3" />
    </Icono>
  )
}

export function IconoTour() {
  return (
    <Icono>
      <path d="m3 20 6-10 4 6 2-3 6 7z" />
      <circle cx="17" cy="6" r="2" />
    </Icono>
  )
}

export function IconoAlquiler() {
  return (
    <Icono>
      <circle cx="6" cy="17" r="3.5" />
      <circle cx="18" cy="17" r="3.5" />
      <path d="M6 17 10 9h5l3 8" />
      <path d="M9 6h3" />
      <path d="m12 17-2-8" />
    </Icono>
  )
}

// --- Condiciones de accesibilidad (RF27, RF50) ---

export function IconoMovilidadReducida() {
  return (
    <Icono>
      <circle cx="11" cy="4" r="1.6" />
      <path d="M11 7v6h5l2 5" />
      <path d="M8 10.5a5.5 5.5 0 1 0 7 7.5" />
    </Icono>
  )
}

export function IconoVisual() {
  return (
    <Icono>
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" />
      <circle cx="12" cy="12" r="3" />
    </Icono>
  )
}

export function IconoAuditiva() {
  return (
    <Icono>
      <path d="M6 8.5a6 6 0 0 1 12 0c0 3.5-3 4.5-3 7.5a3.5 3.5 0 0 1-6 2.5" />
      <path d="M9.5 9a2.5 2.5 0 0 1 5 0" />
    </Icono>
  )
}

export function IconoUbicacion() {
  return (
    <Icono>
      <path d="M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z" />
      <circle cx="12" cy="9.5" r="2.5" />
    </Icono>
  )
}

// --- Registro (WF-02, WF-03) ---

export function IconoEscudo() {
  return (
    <Icono>
      <path d="M12 3 5 6v5c0 4.4 3 8.3 7 9.5 4-1.2 7-5.1 7-9.5V6z" />
    </Icono>
  )
}

export function IconoTelefono() {
  return (
    <Icono>
      <path d="M5 4h3l2 5-2.5 1.5a11 11 0 0 0 6 6L15 14l5 2v3a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2" />
    </Icono>
  )
}

export function IconoInfo() {
  return (
    <Icono>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 11v5M12 8h.01" />
    </Icono>
  )
}

/** Reserva Confirmada. */
export function IconoConfirmada() {
  return (
    <Icono width="16" height="16">
      <circle cx="12" cy="12" r="9" />
      <path d="m8 12.5 2.75 2.75L16 10" />
    </Icono>
  )
}

/** Reserva Cancelada. */
export function IconoCancelada() {
  return (
    <Icono width="16" height="16">
      <circle cx="12" cy="12" r="9" />
      <path d="m9 9 6 6M15 9l-6 6" />
    </Icono>
  )
}

/** Reserva Completada (la actividad ya pasó). */
export function IconoCompletada() {
  return (
    <Icono width="16" height="16">
      <path d="M5 21V4M5 4h11l-2 4 2 4H5" />
    </Icono>
  )
}

/** Calendario (fechas de una reserva). */
export function IconoCalendario() {
  return (
    <Icono width="18" height="18">
      <rect x="3.5" y="5" width="17" height="15" rx="2" />
      <path d="M3.5 10h17M8 3v4M16 3v4" />
    </Icono>
  )
}
