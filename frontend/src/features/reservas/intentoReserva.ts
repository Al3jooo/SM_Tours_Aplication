/**
 * Idempotency-Key del intento de reserva en curso (D6, ajuste 3 del plan). La clave queda asociada al cuerpo exacto
 * del pedido y al turista, y se guarda en sessionStorage para que sobreviva a una recarga: si la primera petición se
 * perdió (error de red, 503) y el turista recarga y confirma la misma selección, sale con la misma clave y ms-booking
 * no reserva dos veces. Una respuesta definitiva (201 o 4xx) la borra. Si sessionStorage no está disponible (modo
 * privado, almacenamiento bloqueado), el registro vive solo en memoria mientras la página esté abierta.
 */
const CLAVE_ALMACEN = 'samaria360.intentoReserva'

interface Intento {
  idUsuario: string
  cuerpo: string
  clave: string
}

let enMemoria: Intento | null = null

function esIntento(valor: unknown): valor is Intento {
  const intento = valor as Intento | null
  return (
    typeof intento === 'object' &&
    intento !== null &&
    typeof intento.idUsuario === 'string' &&
    typeof intento.cuerpo === 'string' &&
    typeof intento.clave === 'string'
  )
}

function leer(): Intento | null {
  try {
    const guardado = sessionStorage.getItem(CLAVE_ALMACEN)
    if (guardado === null) return null
    const intento: unknown = JSON.parse(guardado)
    return esIntento(intento) ? intento : null
  } catch {
    return enMemoria
  }
}

function guardar(intento: Intento | null) {
  enMemoria = intento
  try {
    if (intento) sessionStorage.setItem(CLAVE_ALMACEN, JSON.stringify(intento))
    else sessionStorage.removeItem(CLAVE_ALMACEN)
  } catch {
    // Queda en memoria.
  }
}

/** UUID v4. crypto.randomUUID solo existe en contextos seguros; getRandomValues, en todos. */
function nuevaClave(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID()
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

/**
 * La clave del intento: la guardada si es el mismo turista con el mismo cuerpo; si no (otra selección, otro precio),
 * una nueva que reemplaza a la anterior.
 */
export function claveParaIntento(idUsuario: string, cuerpo: string): string {
  const actual = leer()
  if (actual && actual.idUsuario === idUsuario && actual.cuerpo === cuerpo) return actual.clave
  const nuevo = { idUsuario, cuerpo, clave: nuevaClave() }
  guardar(nuevo)
  return nuevo.clave
}

/**
 * Después de cada respuesta: 201 o 4xx son definitivas y borran el registro. Un error de red (status 0), un 5xx o un
 * 503 no lo son: el registro se conserva para reintentar con la misma clave.
 */
export function terminarIntento(status: number) {
  if (status === 201 || (status >= 400 && status < 500)) guardar(null)
}
