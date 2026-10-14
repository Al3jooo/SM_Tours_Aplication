/** Estado que el registro deja al login: viaja por el router, nunca por la URL (no queda en el historial ni en logs). */
export interface EstadoTrasRegistro {
  registro: 'turista' | 'prestador'
  correo: string
}

export function esEstadoTrasRegistro(estado: unknown): estado is EstadoTrasRegistro {
  if (!estado || typeof estado !== 'object') return false
  const { registro, correo } = estado as Record<string, unknown>
  return (registro === 'turista' || registro === 'prestador') && typeof correo === 'string'
}
