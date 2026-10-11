// El token vive en sessionStorage: se borra al cerrar la pestaña y no se comparte entre pestañas.
const CLAVE = 'samaria360.token'

export function leerToken(): string | null {
  try {
    return sessionStorage.getItem(CLAVE)
  } catch {
    return null
  }
}

export function guardarToken(token: string): void {
  try {
    sessionStorage.setItem(CLAVE, token)
  } catch {
    // Almacenamiento bloqueado: la sesión dura solo mientras la página esté abierta.
  }
}

export function borrarToken(): void {
  try {
    sessionStorage.removeItem(CLAVE)
  } catch {
    // Nada que borrar.
  }
}
