/**
 * Destino después del login, o al entrar a /login con la sesión iniciada: la ruta de origen (state.desde) solo si es
 * una ruta interna de la app. Debe empezar con «/» y no con «//» ni «/\», porque el navegador los trataría como otro
 * sitio (redirección abierta). Nunca vuelve a /login. En cualquier otro caso, el inicio.
 */
export function destinoTrasLogin(desde: unknown): string {
  const interna =
    typeof desde === 'string' && desde.startsWith('/') && !desde.startsWith('//') && !desde.startsWith('/\\')
  return interna && !/^\/login(?:[/?#]|$)/.test(desde) ? desde : '/'
}
