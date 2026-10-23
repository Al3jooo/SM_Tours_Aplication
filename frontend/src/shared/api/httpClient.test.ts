import { describe, expect, it, vi } from 'vitest'
import { solicitar } from './httpClient'

/** Respuesta con `json()` espiado, para saber si el cliente intentó leer el cuerpo. */
function respuestaEspiada(status: number, cuerpo: string | null) {
  const respuesta = new Response(cuerpo, { status, headers: cuerpo ? { 'Content-Type': 'application/json' } : {} })
  const json = vi.spyOn(respuesta, 'json')
  vi.stubGlobal('fetch', vi.fn(async () => respuesta))
  return json
}

describe('solicitar: respuestas sin cuerpo (DEC-73)', () => {
  it('un 204 devuelve undefined sin llamar a json()', async () => {
    const json = respuestaEspiada(204, null)

    await expect(solicitar<void>('/api/v1/catalogo/admin/faq/x', { method: 'DELETE' })).resolves.toBeUndefined()
    expect(json).not.toHaveBeenCalled()
  })

  it('un 200 con cuerpo se sigue leyendo como JSON', async () => {
    const json = respuestaEspiada(200, JSON.stringify({ elementos: [] }))

    await expect(solicitar('/api/v1/catalogo/faq', { publica: true })).resolves.toEqual({ elementos: [] })
    expect(json).toHaveBeenCalledTimes(1)
  })
})
