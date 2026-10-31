import { afterEach, describe, expect, it, vi } from 'vitest'
import { solicitar } from './httpClient'

/** fetch espiado que responde 200 con `{}`; devuelve las llamadas para revisar cabeceras y cuerpo. */
function fetchEspiado(status = 200) {
  const espia = vi.fn(async () => new Response(status === 204 ? null : '{}', { status }))
  vi.stubGlobal('fetch', espia)
  return espia
}

function llamada(espia: ReturnType<typeof fetchEspiado>) {
  const [, init] = espia.mock.calls[0] as unknown as [string, RequestInit]
  return { cabeceras: init.headers as Record<string, string>, cuerpo: init.body }
}

afterEach(() => {
  sessionStorage.clear()
})

describe('solicitar con formulario (DEC-230, registro multipart de atractivos)', () => {
  it('envía el FormData tal cual, sin Content-Type (lo pone el navegador con su boundary) y con el Bearer', async () => {
    sessionStorage.setItem('samaria360.token', 'token-gestor')
    const espia = fetchEspiado()
    const formulario = new FormData()
    formulario.append('datos', new Blob(['{"nombre":"Cascada"}'], { type: 'application/json' }))

    await solicitar('/api/v1/catalogo/gestor/atractivos', { method: 'POST', formulario })

    const { cabeceras, cuerpo } = llamada(espia)
    expect(cuerpo).toBe(formulario)
    expect(cabeceras['Content-Type']).toBeUndefined()
    expect(cabeceras.Authorization).toBe('Bearer token-gestor')
    expect(cabeceras.Accept).toBe('application/json')
  })
})

describe('regresión de las solicitudes JSON (C4)', () => {
  it('cuerpo: se serializa con JSON.stringify y lleva Content-Type application/json', async () => {
    const espia = fetchEspiado()

    await solicitar('/api/v1/catalogo/gestor/atractivos/x', { method: 'PUT', cuerpo: { nombre: 'Cascada', version: 2 } })

    const { cabeceras, cuerpo } = llamada(espia)
    expect(cuerpo).toBe('{"nombre":"Cascada","version":2}')
    expect(cabeceras['Content-Type']).toBe('application/json')
  })

  it('cuerpoTexto: se envía tal cual, con Content-Type application/json', async () => {
    const espia = fetchEspiado()

    await solicitar('/api/v1/catalogo/prestador/servicios/x/precio', { method: 'PUT', cuerpoTexto: '{"precioBase":1.10}' })

    const { cabeceras, cuerpo } = llamada(espia)
    expect(cuerpo).toBe('{"precioBase":1.10}')
    expect(cabeceras['Content-Type']).toBe('application/json')
  })

  it('sin cuerpo: ni body ni Content-Type; una ruta pública sale sin Authorization aunque haya sesión', async () => {
    sessionStorage.setItem('samaria360.token', 'token-turista')
    const espia = fetchEspiado()

    await solicitar('/api/v1/catalogo/atractivos/x', { publica: true })

    const { cabeceras, cuerpo } = llamada(espia)
    expect(cuerpo).toBeUndefined()
    expect(cabeceras['Content-Type']).toBeUndefined()
    expect(cabeceras.Authorization).toBeUndefined()
  })

  it('un 204 sigue devolviendo undefined', async () => {
    fetchEspiado(204)

    await expect(solicitar('/api/v1/catalogo/admin/faq/x', { method: 'DELETE' })).resolves.toBeUndefined()
  })
})
