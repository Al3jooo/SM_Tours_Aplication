// El token vive en sessionStorage: se borra al cerrar la pestaña y no se comparte entre pestañas.
const CLAVE = 'samaria360.token'
// Vencimiento del token (`expiraEn` del login, DEC-48), en milisegundos desde la época, junto al token.
const CLAVE_VENCIMIENTO = 'samaria360.expiraEn'

export function leerToken(): string | null {
  try {
    return sessionStorage.getItem(CLAVE)
  } catch {
    return null
  }
}

/** Sin `vencimiento` (o con uno inválido), la sesión solo se cierra al recibir un 401. */
export function guardarToken(token: string, vencimiento?: number | null): void {
  try {
    sessionStorage.setItem(CLAVE, token)
    if (vencimiento !== undefined && vencimiento !== null && Number.isFinite(vencimiento)) {
      sessionStorage.setItem(CLAVE_VENCIMIENTO, String(vencimiento))
    } else {
      sessionStorage.removeItem(CLAVE_VENCIMIENTO)
    }
  } catch {
    // Almacenamiento bloqueado: la sesión dura solo mientras la página esté abierta.
  }
}

/** Instante (ms) en que vence el token guardado, o null si no se conoce. */
export function leerVencimiento(): number | null {
  try {
    const guardado = sessionStorage.getItem(CLAVE_VENCIMIENTO)
    if (guardado === null) return null
    const vencimiento = Number(guardado)
    return Number.isFinite(vencimiento) ? vencimiento : null
  } catch {
    return null
  }
}

export function borrarToken(): void {
  try {
    sessionStorage.removeItem(CLAVE)
    sessionStorage.removeItem(CLAVE_VENCIMIENTO)
  } catch {
    // Nada que borrar.
  }
}
