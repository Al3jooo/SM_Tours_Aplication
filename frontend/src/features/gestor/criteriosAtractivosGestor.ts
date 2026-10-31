/**
 * Criterio de WF-24 en la URL: solo `pagina` (desde 1, como WF-18; la API empieza en 0). El panel no tiene filtros
 * porque el contrato no los admite (DEC-222). Un valor que no es válido se lee como 1, que no se escribe.
 */
export interface CriteriosAtractivos {
  pagina: number
}

export function leerCriteriosAtractivos(parametros: URLSearchParams): CriteriosAtractivos {
  const pagina = Number(parametros.get('pagina'))
  return { pagina: Number.isInteger(pagina) && pagina >= 1 ? pagina : 1 }
}

export function escribirCriteriosAtractivos(criterios: CriteriosAtractivos): URLSearchParams {
  const parametros = new URLSearchParams()
  if (criterios.pagina > 1) parametros.set('pagina', String(criterios.pagina))
  return parametros
}

/** Anuncio que el panel muestra una vez al volver (registro, edición o un atractivo que ya no existe). */
export interface EstadoAnuncioGestor {
  anuncio?: 'registrado' | 'guardado' | 'noEncontrado'
}
