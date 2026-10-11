import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { HOTEL, paginaOferta, respuestaJson, simularFetch } from '../../test/fetchSimulado'
import { renderApp } from '../../test/renderApp'

// La descarga diferida de la página espera a que la prueba la libere: así el estado intermedio es determinista.
const descarga = vi.hoisted(() => {
  let liberar!: () => void
  const liberada = new Promise<void>((resolver) => {
    liberar = resolver
  })
  return { liberada, liberar }
})

vi.mock('../../features/catalogo/ExplorarPage', async (importOriginal) => {
  await descarga.liberada
  return importOriginal()
})

describe('Carga diferida de /explorar', () => {
  // Límite de 15 s: medida en más de 2 s (única del archivo: primer render y carga diferida de /explorar).
  it('mientras llega la página se ve el estado de carga del catálogo, con el encabezado visible', async () => {
    simularFetch({ 'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([HOTEL])) })
    renderApp('/explorar')

    const carga = (await screen.findByText('Buscando lugares…')).closest('[role="status"]')
    expect(carga).toHaveAttribute('aria-busy', 'true')
    expect(screen.queryByRole('heading', { level: 1 })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Inicio de sesión' })).toBeInTheDocument()

    descarga.liberar()

    expect(await screen.findByRole('heading', { level: 1, name: /Exploración de Atractivos/ })).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Hotel Casa Tayrona' })).toBeInTheDocument()
  }, 15_000)
})
