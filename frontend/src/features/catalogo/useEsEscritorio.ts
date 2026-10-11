import { useSyncExternalStore } from 'react'

/** Mismo punto de corte que catalogo.css: desde aquí lista y mapa caben lado a lado. */
const CONSULTA_ESCRITORIO = '(min-width: 64rem)'

const consulta = () => (typeof window.matchMedia === 'function' ? window.matchMedia(CONSULTA_ESCRITORIO) : null)

function suscribir(avisar: () => void) {
  const media = consulta()
  media?.addEventListener('change', avisar)
  return () => media?.removeEventListener('change', avisar)
}

/** Verdadero con dos columnas (escritorio). Sin matchMedia (p. ej. jsdom) se asume una columna. */
export function useEsEscritorio(): boolean {
  return useSyncExternalStore(suscribir, () => consulta()?.matches ?? false)
}
