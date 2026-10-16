/**
 * Ruta interna de la app: debe empezar con «/» y no con «//» ni «/\», porque el navegador los trataría como otro sitio
 * (redirección abierta). Se usa para cualquier destino que llega en el estado de la navegación.
 */
export function esRutaInterna(ruta: unknown): ruta is string {
  return typeof ruta === 'string' && ruta.startsWith('/') && !ruta.startsWith('//') && !ruta.startsWith('/\\')
}

/**
 * Destino después del login, o al entrar a /login con la sesión iniciada: la ruta de origen (state.desde) solo si es
 * una ruta interna de la app. Nunca vuelve a /login. En cualquier otro caso, el inicio.
 */
export function destinoTrasLogin(desde: unknown): string {
  return esRutaInterna(desde) && !/^\/login(?:[/?#]|$)/.test(desde) ? desde : '/'
}
